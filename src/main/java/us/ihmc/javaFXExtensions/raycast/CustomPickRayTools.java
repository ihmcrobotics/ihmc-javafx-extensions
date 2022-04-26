package us.ihmc.javaFXExtensions.raycast;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.PickResult;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import us.ihmc.euclid.exceptions.SingularMatrixException;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;

/**
 * Workaround to enable raycast to be done manually.
 * <p>
 * Large code duplication with JavaFX 8.
 * </p>
 * <p>
 * The implementation could be revisited to rely more heavily onto euclid as it seems some of the
 * operation here are not super optimized. Ideally, a future version of JavaFX would make this class
 * obsolete.
 * </p>
 */
public class CustomPickRayTools
{
   public static PickResult pick(double xScene, double yScene, double widthScene, double heightScene, PerspectiveCamera camera, Node root)
   {
      CustomPickRay pickRay = computePickRay(xScene, yScene, widthScene, heightScene, camera, null);
      pickRay.direction.normalize();
      CustomPickResultChooser result = new CustomPickResultChooser();
      pickNode(root, pickRay, result);
      return result.toPickResult();
   }

   public static CustomPickRay computePickRay(double xScene,
                                              double yScene,
                                              double widthScene,
                                              double heightScene,
                                              PerspectiveCamera camera,
                                              CustomPickRay pickRay)
   {
      return computePerspectivePickRay(xScene,
                                       yScene,
                                       widthScene,
                                       heightScene,
                                       camera.isFixedEyeAtCameraZero(),
                                       Math.toRadians(camera.getFieldOfView()),
                                       camera.isVerticalFieldOfView(),
                                       camera.getLocalToSceneTransform(),
                                       camera.getNearClip(),
                                       camera.getFarClip(),
                                       pickRay);
   }

   public static CustomPickRay computePerspectivePickRay(double xScene,
                                                         double yScene,
                                                         double widthScene,
                                                         double heightScene,
                                                         boolean fixedEye,
                                                         double fieldOfViewRadians,
                                                         boolean verticalFieldOfView,
                                                         Transform cameraTransform,
                                                         double nearClip,
                                                         double farClip,
                                                         CustomPickRay pickRay)
   {
      if (pickRay == null)
         pickRay = new CustomPickRay();

      Vector3D direction = pickRay.direction;
      double halfViewWidth = widthScene / 2.0;
      double halfViewHeight = heightScene / 2.0;
      double halfViewDim = verticalFieldOfView ? halfViewHeight : halfViewWidth;
      // Distance to projection plane from eye
      double distanceZ = halfViewDim / Math.tan(fieldOfViewRadians / 2.0);

      direction.set(xScene - halfViewWidth, yScene - halfViewHeight, distanceZ);

      Point3D eye = pickRay.origin;

      if (fixedEye)
         eye.setToZero();
      else // set eye at center of viewport and move back so that projection plane is at Z = 0
         eye.set(halfViewWidth, halfViewHeight, -distanceZ);

      double clipScale = direction.length() / (fixedEye ? distanceZ : 1.0);
      pickRay.nearClip = nearClip * clipScale;
      pickRay.farClip = farClip * clipScale;

      pickRay.applyTransform(cameraTransform);

      return pickRay;
   }

   public static void pickNode(Node node, CustomPickRay pickRay, CustomPickResultChooser result)
   {
      // In some conditions we can omit picking this node or subgraph
      if (!node.isVisible() || node.isDisable() || node.isMouseTransparent())
      {
         return;
      }

      final Point3D o = pickRay.origin;
      final double ox = o.getX();
      final double oy = o.getY();
      final double oz = o.getZ();
      final Vector3D d = pickRay.direction;
      final double dx = d.getX();
      final double dy = d.getY();
      final double dz = d.getZ();

      Transform localToParentTransform = node.getLocalToParentTransform();
      try
      {
         inverseTransform(localToParentTransform, o);
         inverseTransform(localToParentTransform, d);

         // Delegate to a function which can be overridden by subclasses which
         // actually does the pick. The implementation is markedly different
         // for leaf nodes vs. parent nodes vs. region nodes.
         pickNodeLocal(node, pickRay, result);
      }
      catch (SingularMatrixException e)
      {
         // in this case we just don't pick anything
      }

      o.set(ox, oy, oz);
      d.set(dx, dy, dz);
   }

   public static void pickNodeLocal(Node node, CustomPickRay pickRay, CustomPickResultChooser pickResult)
   {
      if (node instanceof Parent)
         pickNodeLocal((Parent) node, pickRay, pickResult);
      else
         intersects(node, pickRay, pickResult);
   }

   public static void pickNodeLocal(Parent node, CustomPickRay pickRay, CustomPickResultChooser pickResult)
   {
      double boundsDistance = intersectsBounds(node, pickRay);

      if (!Double.isNaN(boundsDistance))
      {
         ObservableList<Node> children = node.getChildrenUnmodifiable();

         for (int i = children.size() - 1; i >= 0; i--)
         {
            pickNode(children.get(i), pickRay, pickResult);

            if (pickResult.isClosed())
            {
               return;
            }
         }

         if (node.isPickOnBounds())
         {
            pickResult.offer(node, boundsDistance, CustomPickResultChooser.computePoint(pickRay, boundsDistance));
         }
      }
   }

   public static boolean intersects(Node node, CustomPickRay pickRay, CustomPickResultChooser pickResult)
   {
      double boundsDistance = intersectsBounds(node, pickRay);
      if (!Double.isNaN(boundsDistance))
      {
         if (node.isPickOnBounds())
         {
            if (pickResult != null)
            {
               pickResult.offer(node, boundsDistance, CustomPickResultChooser.computePoint(pickRay, boundsDistance));
            }
            return true;
         }
         else
         {
            return computeIntersects(node, pickRay, pickResult);
         }
      }
      return false;
   }

   /**
    * Computes the intersection of the pickRay with this node. The pickResult argument is updated if
    * the found intersection is closer than the passed one. On the other hand, the return value
    * specifies whether the intersection exists, regardless of its comparison with the given
    * pickResult.
    *
    * @treatAsPrivate implementation detail
    * @deprecated This is an internal API that is not intended for use and will be removed in the next
    *             version
    */
   @Deprecated
   public static boolean computeIntersects(Node node, CustomPickRay pickRay, CustomPickResultChooser pickResult)
   {
      if (node instanceof MeshView)
      {
         MeshView meshView = (MeshView) node;

         if (meshView.getMesh() instanceof TriangleMesh)
         {
            return computeIntersects((TriangleMesh) meshView.getMesh(), pickRay, pickResult, meshView, meshView.getCullFace(), true);
         }
      }
      if (node instanceof Box)
      {
         return computeIntersects((Box) node, pickRay, pickResult);
      }
      if (node instanceof Sphere)
      {
         return computeIntersects((Sphere) node, pickRay, pickResult);
      }
      if (node instanceof Cylinder)
      {
         return computeIntersects((Cylinder) node, pickRay, pickResult);
      }

      double origZ = pickRay.origin.getZ();
      double dirZ = pickRay.direction.getZ();
      // Handle the case where pickRay is almost parallel to the Z-plane
      if (almostZero(dirZ))
      {
         return false;
      }
      double t = -origZ / dirZ;
      if (t < pickRay.nearClip || t > pickRay.farClip)
      {
         return false;
      }

      double x = pickRay.origin.getX() + (pickRay.direction.getX() * t);
      double y = pickRay.origin.getY() + (pickRay.direction.getY() * t);

      if (node.contains((float) x, (float) y))
      {
         if (pickResult != null)
         {
            pickResult.offer(node, t, CustomPickResultChooser.computePoint(pickRay, t));
         }
         return true;
      }
      return false;
   }

   @Deprecated
   public static boolean computeIntersects(Sphere node, CustomPickRay pickRay, CustomPickResultChooser pickResult)
   {
      final double r = node.getRadius();
      final Vector3D dir = pickRay.direction;
      final double dirX = dir.getX();
      final double dirY = dir.getY();
      final double dirZ = dir.getZ();
      final Point3D origin = pickRay.origin;
      final double originX = origin.getX();
      final double originY = origin.getY();
      final double originZ = origin.getZ();

      // Coeficients of a quadratic equation desribing intersection with sphere
      final double a = dirX * dirX + dirY * dirY + dirZ * dirZ;
      final double b = 2 * (dirX * originX + dirY * originY + dirZ * originZ);
      final double c = originX * originX + originY * originY + originZ * originZ - r * r;

      final double discriminant = b * b - 4 * a * c;
      if (discriminant < 0)
      {
         // No real roots of the equation, missed the shape
         return false;
      }

      final double distSqrt = Math.sqrt(discriminant);
      final double q = (b < 0) ? (-b - distSqrt) / 2.0 : (-b + distSqrt) / 2.0;

      double t0 = q / a;
      double t1 = c / q;

      if (t0 > t1)
      {
         final double temp = t0;
         t0 = t1;
         t1 = temp;
      }

      final double minDistance = pickRay.nearClip;
      final double maxDistance = pickRay.farClip;

      if (t1 < minDistance || t0 > maxDistance)
      {
         // the sphere is out of clipping planes
         return false;
      }

      double t = t0;
      final CullFace cullFace = node.getCullFace();
      if (t0 < minDistance || cullFace == CullFace.FRONT)
      {
         if (t1 <= maxDistance && node.getCullFace() != CullFace.BACK)
         {
            // picking the back wall
            t = t1;
         }
         else
         {
            // we are inside the sphere with the back wall culled, but the
            // exact picking still needs to be done because the front faced
            // triangles may still be in front of us
            return false;
         }
      }

      if (Double.isInfinite(t) || Double.isNaN(t))
      {
         // We've got a nonsense pick ray or sphere size.
         return false;
      }

      if (pickResult != null && pickResult.isCloser(t))
      {
         final javafx.geometry.Point3D point = CustomPickResultChooser.computePoint(pickRay, t);

         // computing texture coords
         final javafx.geometry.Point3D proj = new javafx.geometry.Point3D(point.getX(), 0, point.getZ());
         final javafx.geometry.Point3D cross = proj.crossProduct(Rotate.Z_AXIS);
         double angle = proj.angle(Rotate.Z_AXIS);
         if (cross.getY() > 0)
         {
            angle = 360 - angle;
         }
         javafx.geometry.Point2D txtCoords = new javafx.geometry.Point2D(1 - angle / 360, 0.5 + point.getY() / (2 * r));

         pickResult.offer(node, t, PickResult.FACE_UNDEFINED, point, txtCoords);
      }
      return true;
   }

   /**
    * @treatAsPrivate implementation detail
    * @deprecated This is an internal API that is not intended for use and will be removed in the next
    *             version
    */
   @Deprecated
   public static boolean computeIntersects(Cylinder node, CustomPickRay pickRay, CustomPickResultChooser pickResult)
   {
      final double r = node.getRadius();
      final Vector3D dir = pickRay.direction;
      final double dirX = dir.getX();
      final double dirY = dir.getY();
      final double dirZ = dir.getZ();
      final Point3D origin = pickRay.origin;
      final double originX = origin.getX();
      final double originY = origin.getY();
      final double originZ = origin.getZ();
      final double h = node.getHeight();
      final double halfHeight = h / 2.0;
      final CullFace cullFace = node.getCullFace();

      // Check the open cylinder first

      // Coeficients of a quadratic equation desribing intersection with an infinite cylinder
      final double a = dirX * dirX + dirZ * dirZ;
      final double b = 2 * (dirX * originX + dirZ * originZ);
      final double c = originX * originX + originZ * originZ - r * r;

      final double discriminant = b * b - 4 * a * c;

      double t0, t1, t = Double.POSITIVE_INFINITY;
      final double minDistance = pickRay.nearClip;
      final double maxDistance = pickRay.farClip;

      if (discriminant >= 0 && (dirX != 0.0 || dirZ != 0.0))
      {
         // the line hits the infinite cylinder

         final double distSqrt = Math.sqrt(discriminant);
         final double q = (b < 0) ? (-b - distSqrt) / 2.0 : (-b + distSqrt) / 2.0;

         t0 = q / a;
         t1 = c / q;

         if (t0 > t1)
         {
            double temp = t0;
            t0 = t1;
            t1 = temp;
         }

         // let's see if the hit is between clipping planes and within the cylinder's height
         final double y0 = originY + t0 * dirY;
         if (t0 < minDistance || y0 < -halfHeight || y0 > halfHeight || cullFace == CullFace.FRONT)
         {
            final double y1 = originY + t1 * dirY;
            if (t1 >= minDistance && t1 <= maxDistance && y1 >= -halfHeight && y1 <= halfHeight)
            {
               if (cullFace != CullFace.BACK)
               {
                  // t0 is outside or behind but t1 hits.

                  // We need to do the exact picking even if the back wall
                  // is culled because the front facing triangles may
                  // still be in front of us
                  t = t1;
               }
            } // else no hit (but we need to check the caps)
         }
         else if (t0 <= maxDistance)
         {
            // t0 hits the height between clipping planes
            t = t0;
         } // else no hit (but we need to check the caps)
      }

      // Now check the caps

      // if we already know we are going to do the exact picking,
      // there is no need to check the caps

      boolean topCap = false, bottomCap = false;
      final double tBottom = (-halfHeight - originY) / dirY;
      final double tTop = (halfHeight - originY) / dirY;
      boolean isT0Bottom = false;

      if (tBottom < tTop)
      {
         t0 = tBottom;
         t1 = tTop;
         isT0Bottom = true;
      }
      else
      {
         t0 = tTop;
         t1 = tBottom;
      }

      if (t0 >= minDistance && t0 <= maxDistance && t0 < t && cullFace != CullFace.FRONT)
      {
         final double tX = originX + dirX * t0;
         final double tZ = originZ + dirZ * t0;
         if (tX * tX + tZ * tZ <= r * r)
         {
            bottomCap = isT0Bottom;
            topCap = !isT0Bottom;
            t = t0;
         }
      }

      if (t1 >= minDistance && t1 <= maxDistance && t1 < t && cullFace != CullFace.BACK)
      {
         final double tX = originX + dirX * t1;
         final double tZ = originZ + dirZ * t1;
         if (tX * tX + tZ * tZ <= r * r)
         {
            topCap = isT0Bottom;
            bottomCap = !isT0Bottom;
            t = t1;
         }
      }

      if (Double.isInfinite(t) || Double.isNaN(t))
      {
         // no hit
         return false;
      }

      if (pickResult != null && pickResult.isCloser(t))
      {
         final javafx.geometry.Point3D point = CustomPickResultChooser.computePoint(pickRay, t);

         javafx.geometry.Point2D txCoords;
         if (topCap)
         {
            txCoords = new javafx.geometry.Point2D(0.5 + point.getX() / (2 * r), 0.5 + point.getZ() / (2 * r));
         }
         else if (bottomCap)
         {
            txCoords = new javafx.geometry.Point2D(0.5 + point.getX() / (2 * r), 0.5 - point.getZ() / (2 * r));
         }
         else
         {
            final javafx.geometry.Point3D proj = new javafx.geometry.Point3D(point.getX(), 0, point.getZ());
            final javafx.geometry.Point3D cross = proj.crossProduct(Rotate.Z_AXIS);
            double angle = proj.angle(Rotate.Z_AXIS);
            if (cross.getY() > 0)
            {
               angle = 360 - angle;
            }
            txCoords = new javafx.geometry.Point2D(1 - angle / 360, 0.5 + point.getY() / h);
         }

         pickResult.offer(node, t, PickResult.FACE_UNDEFINED, point, txCoords);
      }
      return true;
   }

   @Deprecated
   public static boolean computeIntersects(Box node, CustomPickRay pickRay, CustomPickResultChooser pickResult)
   {

      final double w = node.getWidth();
      final double h = node.getHeight();
      final double d = node.getDepth();
      final double hWidth = w / 2.0;
      final double hHeight = h / 2.0;
      final double hDepth = d / 2.0;
      final Vector3D dir = pickRay.direction;
      final double invDirX = dir.getX() == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.getX());
      final double invDirY = dir.getY() == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.getY());
      final double invDirZ = dir.getZ() == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.getZ());
      final Point3D origin = pickRay.origin;
      final double originX = origin.getX();
      final double originY = origin.getY();
      final double originZ = origin.getZ();
      final boolean signX = invDirX < 0.0;
      final boolean signY = invDirY < 0.0;
      final boolean signZ = invDirZ < 0.0;

      double t0 = Double.NEGATIVE_INFINITY;
      double t1 = Double.POSITIVE_INFINITY;
      char side0 = '0';
      char side1 = '0';

      if (Double.isInfinite(invDirX))
      {
         if (-hWidth <= originX && hWidth >= originX)
         {
            // move on, we are inside for the whole length
         }
         else
         {
            return false;
         }
      }
      else
      {
         t0 = ((signX ? hWidth : -hWidth) - originX) * invDirX;
         t1 = ((signX ? -hWidth : hWidth) - originX) * invDirX;
         side0 = signX ? 'X' : 'x';
         side1 = signX ? 'x' : 'X';
      }

      if (Double.isInfinite(invDirY))
      {
         if (-hHeight <= originY && hHeight >= originY)
         {
            // move on, we are inside for the whole length
         }
         else
         {
            return false;
         }
      }
      else
      {
         final double ty0 = ((signY ? hHeight : -hHeight) - originY) * invDirY;
         final double ty1 = ((signY ? -hHeight : hHeight) - originY) * invDirY;

         if ((t0 > ty1) || (ty0 > t1))
         {
            return false;
         }
         if (ty0 > t0)
         {
            side0 = signY ? 'Y' : 'y';
            t0 = ty0;
         }
         if (ty1 < t1)
         {
            side1 = signY ? 'y' : 'Y';
            t1 = ty1;
         }
      }

      if (Double.isInfinite(invDirZ))
      {
         if (-hDepth <= originZ && hDepth >= originZ)
         {
            // move on, we are inside for the whole length
         }
         else
         {
            return false;
         }
      }
      else
      {
         double tz0 = ((signZ ? hDepth : -hDepth) - originZ) * invDirZ;
         double tz1 = ((signZ ? -hDepth : hDepth) - originZ) * invDirZ;

         if ((t0 > tz1) || (tz0 > t1))
         {
            return false;
         }
         if (tz0 > t0)
         {
            side0 = signZ ? 'Z' : 'z';
            t0 = tz0;
         }
         if (tz1 < t1)
         {
            side1 = signZ ? 'z' : 'Z';
            t1 = tz1;
         }
      }

      char side = side0;
      double t = t0;
      final CullFace cullFace = node.getCullFace();
      final double minDistance = pickRay.nearClip;
      final double maxDistance = pickRay.farClip;

      if (t0 > maxDistance)
      {
         return false;
      }
      if (t0 < minDistance || cullFace == CullFace.FRONT)
      {
         if (t1 >= minDistance && t1 <= maxDistance && cullFace != CullFace.BACK)
         {
            side = side1;
            t = t1;
         }
         else
         {
            return false;
         }
      }

      if (Double.isInfinite(t) || Double.isNaN(t))
      {
         // We've got a nonsense pick ray or box size.
         return false;
      }

      if (pickResult != null && pickResult.isCloser(t))
      {
         javafx.geometry.Point3D point = CustomPickResultChooser.computePoint(pickRay, t);

         javafx.geometry.Point2D txtCoords = null;

         switch (side)
         {
            case 'x': // left
               txtCoords = new javafx.geometry.Point2D(0.5 - point.getZ() / d, 0.5 + point.getY() / h);
               break;
            case 'X': // right
               txtCoords = new javafx.geometry.Point2D(0.5 + point.getZ() / d, 0.5 + point.getY() / h);
               break;
            case 'y': // top
               txtCoords = new javafx.geometry.Point2D(0.5 + point.getX() / w, 0.5 - point.getZ() / d);
               break;
            case 'Y': // bottom
               txtCoords = new javafx.geometry.Point2D(0.5 + point.getX() / w, 0.5 + point.getZ() / d);
               break;
            case 'z': // front
               txtCoords = new javafx.geometry.Point2D(0.5 + point.getX() / w, 0.5 + point.getY() / h);
               break;
            case 'Z': // back
               txtCoords = new javafx.geometry.Point2D(0.5 - point.getX() / w, 0.5 + point.getY() / h);
               break;
            default:
               // No hit with any of the planes. We must have had a zero
               // pick ray direction vector. Should never happen.
               return false;
         }

         pickResult.offer(node, t, PickResult.FACE_UNDEFINED, point, txtCoords);
      }

      return true;
   }

   /**
    * @treatAsPrivate implementation detail
    * @deprecated This is an internal API that is not intended for use and will be removed in the next
    *             version
    */
   @Deprecated
   public static boolean computeIntersects(TriangleMesh triangleMesh,
                                           CustomPickRay pickRay,
                                           CustomPickResultChooser pickResult,
                                           Node candidate,
                                           CullFace cullFace,
                                           boolean reportFace)
   {

      boolean found = false;
      //      if (triangleMesh.validate())
      {
         final int size = triangleMesh.getFaces().size();

         for (int i = 0; i < size; i += triangleMesh.getFaceElementSize())
         {
            if (computeIntersectsFace(triangleMesh, pickRay, i, cullFace, candidate, reportFace, pickResult))
            {
               found = true;
            }
         }
      }
      return found;
   }

   /**
    * Computes intersection of a pick ray and a single triangle face. It takes pickRay, origin and dir.
    * The latter two can be of course obtained from the pickRay, but we need them to be converted to
    * Point3D and don't want to do that for all faces. Therefore the conversion is done just once and
    * passed to the method for all the faces.
    *
    * @param pickRay    pick ray
    * @param origin     pick ray's origin
    * @param dir        pick ray's direction
    * @param faceIndex  index of the face to test
    * @param cullFace   cull face of the Node (and thus the tested face)
    * @param candidate  the owner node (for the possible placement to the result)
    * @param reportFace whether or not to report he hit face
    * @param result     the pick result to be updated if a closer intersection is found
    * @return true if the pick ray intersects with the face (regardless of whether the result has been
    *         updated)
    */
   public static boolean computeIntersectsFace(TriangleMesh triangleMesh,
                                               CustomPickRay pickRay,
                                               int faceIndex,
                                               CullFace cullFace,
                                               Node candidate,
                                               boolean reportFace,
                                               CustomPickResultChooser result)
   {//, BoxBounds rayBounds) {
      Point3D origin = pickRay.origin;
      Vector3D dir = pickRay.direction;

      // This computation was naturally done by Point3D and its operations,
      // but it needs a lot of points and there is often a lot of triangles
      // so it is vital for performance to use only primitive variables
      // and do the computing manually.

      int vertexIndexSize = triangleMesh.getVertexFormat().getVertexIndexSize();
      int pointElementSize = 3; // TODO triangleMesh.getVertexFormat().getPointElementSize();
      ObservableFaceArray faces = triangleMesh.getFaces();
      final int v0Idx = faces.get(faceIndex) * pointElementSize;
      final int v1Idx = faces.get(faceIndex + vertexIndexSize) * pointElementSize;
      final int v2Idx = faces.get(faceIndex + (2 * vertexIndexSize)) * pointElementSize;

      ObservableFloatArray points = triangleMesh.getPoints();
      final float v0x = points.get(v0Idx);
      final float v0y = points.get(v0Idx + 1);
      final float v0z = points.get(v0Idx + 2);
      final float v1x = points.get(v1Idx);
      final float v1y = points.get(v1Idx + 1);
      final float v1z = points.get(v1Idx + 2);
      final float v2x = points.get(v2Idx);
      final float v2y = points.get(v2Idx + 1);
      final float v2z = points.get(v2Idx + 2);

      // e1 = v1.subtract(v0)
      final float e1x = v1x - v0x;
      final float e1y = v1y - v0y;
      final float e1z = v1z - v0z;
      // e2 = v2.subtract(v0)
      final float e2x = v2x - v0x;
      final float e2y = v2y - v0y;
      final float e2z = v2z - v0z;

      // h = dir.crossProduct(e2)
      final double hx = dir.getY() * e2z - dir.getZ() * e2y;
      final double hy = dir.getZ() * e2x - dir.getX() * e2z;
      final double hz = dir.getX() * e2y - dir.getY() * e2x;

      // a = e1.dotProduct(h)
      final double a = e1x * hx + e1y * hy + e1z * hz;
      if (a == 0.0)
      {
         return false;
      }
      final double f = 1.0 / a;

      // s = origin.subtract(v0)
      final double sx = origin.getX() - v0x;
      final double sy = origin.getY() - v0y;
      final double sz = origin.getZ() - v0z;

      // u = f * (s.dotProduct(h))
      final double u = f * (sx * hx + sy * hy + sz * hz);

      if (u < 0.0 || u > 1.0)
      {
         return false;
      }

      // q = s.crossProduct(e1)
      final double qx = sy * e1z - sz * e1y;
      final double qy = sz * e1x - sx * e1z;
      final double qz = sx * e1y - sy * e1x;

      // v = f * dir.dotProduct(q)
      double v = f * (dir.getX() * qx + dir.getY() * qy + dir.getZ() * qz);

      if (v < 0.0 || u + v > 1.0)
      {
         return false;
      }

      // t = f * e2.dotProduct(q)
      final double t = f * (e2x * qx + e2y * qy + e2z * qz);

      if (t >= pickRay.nearClip && t <= pickRay.farClip)
      {
         // This branch is entered only for hit triangles (not so often),
         // so we can get smoothly back to the nice code using Point3Ds.

         if (cullFace != CullFace.NONE)
         {
            // normal = e1.crossProduct(e2)
            final javafx.geometry.Point3D normal = new javafx.geometry.Point3D(e1y * e2z - e1z * e2y, e1z * e2x - e1x * e2z, e1x * e2y - e1y * e2x);

            final double nangle = normal.angle(-dir.getX(), -dir.getY(), -dir.getZ());
            if ((nangle >= 90 || cullFace != CullFace.BACK) && (nangle <= 90 || cullFace != CullFace.FRONT))
            {
               // hit culled face
               return false;
            }
         }

         if (Double.isInfinite(t) || Double.isNaN(t))
         {
            // we've got a nonsense pick ray or triangle
            return false;
         }

         if (result == null || !result.isCloser(t))
         {
            // it intersects, but we are not interested in the result
            // or we already have a better (closer) result
            // so we can omit the point and texture computation
            return true;
         }

         javafx.geometry.Point3D point = CustomPickResultChooser.computePoint(pickRay, t);

         // Now compute texture mapping. First rotate the triangle
         // so that we can compute in 2D

         // centroid = computeCentroid(v0, v1, v2);
         final javafx.geometry.Point3D centroid = computeCentroid(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z);

         // cv0 = v0.subtract(centroid)
         final javafx.geometry.Point3D cv0 = new javafx.geometry.Point3D(v0x - centroid.getX(), v0y - centroid.getY(), v0z - centroid.getZ());
         // cv1 = v1.subtract(centroid)
         final javafx.geometry.Point3D cv1 = new javafx.geometry.Point3D(v1x - centroid.getX(), v1y - centroid.getY(), v1z - centroid.getZ());
         // cv2 = v2.subtract(centroid)
         final javafx.geometry.Point3D cv2 = new javafx.geometry.Point3D(v2x - centroid.getX(), v2y - centroid.getY(), v2z - centroid.getZ());

         final javafx.geometry.Point3D ce1 = cv1.subtract(cv0);
         final javafx.geometry.Point3D ce2 = cv2.subtract(cv0);
         javafx.geometry.Point3D n = ce1.crossProduct(ce2);
         if (n.getZ() < 0)
         {
            n = new javafx.geometry.Point3D(-n.getX(), -n.getY(), -n.getZ());
         }
         final javafx.geometry.Point3D ax = n.crossProduct(Rotate.Z_AXIS);
         final double angle = Math.atan2(ax.magnitude(), n.dotProduct(Rotate.Z_AXIS));

         Rotate r = new Rotate(Math.toDegrees(angle), ax);
         final javafx.geometry.Point3D crv0 = r.transform(cv0);
         final javafx.geometry.Point3D crv1 = r.transform(cv1);
         final javafx.geometry.Point3D crv2 = r.transform(cv2);
         final javafx.geometry.Point3D rPoint = r.transform(point.subtract(centroid));

         final javafx.geometry.Point2D flatV0 = new javafx.geometry.Point2D(crv0.getX(), crv0.getY());
         final javafx.geometry.Point2D flatV1 = new javafx.geometry.Point2D(crv1.getX(), crv1.getY());
         final javafx.geometry.Point2D flatV2 = new javafx.geometry.Point2D(crv2.getX(), crv2.getY());
         final javafx.geometry.Point2D flatPoint = new javafx.geometry.Point2D(rPoint.getX(), rPoint.getY());

         // Obtain the texture triangle
         int texCoordElementSize = 2; // TODO triangleMesh.getVertexFormat().getTexCoordElementSize();
         int texCoordOffset = triangleMesh.getVertexFormat().getTexCoordIndexOffset();
         final int t0Idx = faces.get(faceIndex + texCoordOffset) * texCoordElementSize;
         final int t1Idx = faces.get(faceIndex + vertexIndexSize + texCoordOffset) * texCoordElementSize;
         final int t2Idx = faces.get(faceIndex + (vertexIndexSize * 2) + texCoordOffset) * texCoordElementSize;

         ObservableFloatArray texCoords = triangleMesh.getTexCoords();
         final javafx.geometry.Point2D u0 = new javafx.geometry.Point2D(texCoords.get(t0Idx), texCoords.get(t0Idx + 1));
         final javafx.geometry.Point2D u1 = new javafx.geometry.Point2D(texCoords.get(t1Idx), texCoords.get(t1Idx + 1));
         final javafx.geometry.Point2D u2 = new javafx.geometry.Point2D(texCoords.get(t2Idx), texCoords.get(t2Idx + 1));

         final javafx.geometry.Point2D txCentroid = computeCentroid(u0, u1, u2);

         final javafx.geometry.Point2D cu0 = u0.subtract(txCentroid);
         final javafx.geometry.Point2D cu1 = u1.subtract(txCentroid);
         final javafx.geometry.Point2D cu2 = u2.subtract(txCentroid);

         // Find the transform between the two triangles

         final Affine src = new Affine(flatV0.getX(), flatV1.getX(), flatV2.getX(), flatV0.getY(), flatV1.getY(), flatV2.getY());
         final Affine trg = new Affine(cu0.getX(), cu1.getX(), cu2.getX(), cu0.getY(), cu1.getY(), cu2.getY());

         javafx.geometry.Point2D txCoords = null;

         try
         {
            src.invert();
            trg.append(src);
            txCoords = txCentroid.add(trg.transform(flatPoint));
         }
         catch (NonInvertibleTransformException e)
         {
            // Can't compute texture mapping, probably the coordinates
            // don't make sense. Ignore it and return null tex coords.
         }

         result.offer(candidate, t, reportFace ? faceIndex / triangleMesh.getFaceElementSize() : PickResult.FACE_UNDEFINED, point, txCoords);
         return true;
      }

      return false;
   }

   private static final double ONE_THIRD = 1.0 / 3.0;

   /**
    * Computes the centroid of the given triangle
    * 
    * @param v0x x coord of first vertex of the triangle
    * @param v0y y coord of first vertex of the triangle
    * @param v0z z coord of first vertex of the triangle
    * @param v1x x coord of second vertex of the triangle
    * @param v1y y coord of second vertex of the triangle
    * @param v1z z coord of second vertex of the triangle
    * @param v2x x coord of third vertex of the triangle
    * @param v2y y coord of third vertex of the triangle
    * @param v2z z coord of third vertex of the triangle
    * @return the triangle centroid
    */
   private static javafx.geometry.Point3D computeCentroid(double v0x,
                                                          double v0y,
                                                          double v0z,
                                                          double v1x,
                                                          double v1y,
                                                          double v1z,
                                                          double v2x,
                                                          double v2y,
                                                          double v2z)
   {
      return new javafx.geometry.Point3D(v0x + ONE_THIRD * (0.5 * (v1x + v2x) - v0x),
                                         v0y + ONE_THIRD * (0.5 * (v1y + v2y) - v0y),
                                         v0z + ONE_THIRD * (0.5 * (v1z + v2z) - v0z));
   }

   /**
    * Computes the centroid of the given triangle
    * 
    * @param v0 vertex of the triangle
    * @param v1 vertex of the triangle
    * @param v2 vertex of the triangle
    * @return the triangle centroid
    */
   private static javafx.geometry.Point2D computeCentroid(javafx.geometry.Point2D v0, javafx.geometry.Point2D v1, javafx.geometry.Point2D v2)
   {
      return new javafx.geometry.Point2D(v0.getX() + ONE_THIRD * (0.5 * (v1.getX() + v2.getX()) - v0.getX()),
                                         v0.getY() + ONE_THIRD * (0.5 * (v1.getY() + v2.getY()) - v0.getY()));
   }

   /**
    * Computes the intersection of the pickRay with the bounds of this node. The return value is the
    * distance between the camera and the intersection point, measured in pickRay direction magnitudes.
    * If there is no intersection, it returns NaN.
    *
    * @param pickRay The pick ray
    * @return Distance of the intersection point, a NaN if there is no intersection
    * @treatAsPrivate implementation detail
    * @deprecated This is an internal API that is not intended for use and will be removed in the next
    *             version
    */
   @Deprecated
   public static double intersectsBounds(Node node, CustomPickRay pickRay)
   {

      final Vector3D dir = pickRay.direction;
      double tmin, tmax;

      final Point3D origin = pickRay.origin;
      final double originX = origin.getX();
      final double originY = origin.getY();
      final double originZ = origin.getZ();

      Bounds bounds = node.getBoundsInLocal();

      if (dir.getX() == 0.0 && dir.getY() == 0.0)
      {
         // fast path for the usual 2D picking

         if (dir.getZ() == 0.0)
         {
            return Double.NaN;
         }

         if (originX < bounds.getMinX() || originX > bounds.getMaxX() || originY < bounds.getMinY() || originY > bounds.getMaxY())
         {
            return Double.NaN;
         }

         final double invDirZ = 1.0 / dir.getZ();
         final boolean signZ = invDirZ < 0.0;

         final double minZ = bounds.getMinZ();
         final double maxZ = bounds.getMaxZ();
         tmin = ((signZ ? maxZ : minZ) - originZ) * invDirZ;
         tmax = ((signZ ? minZ : maxZ) - originZ) * invDirZ;

      }
      else if (bounds.getDepth() == 0.0)
      {
         // fast path for 3D picking of 2D bounds

         if (almostZero(dir.getZ()))
         {
            return Double.NaN;
         }

         final double t = (bounds.getMinZ() - originZ) / dir.getZ();
         final double x = originX + (dir.getX() * t);
         final double y = originY + (dir.getY() * t);

         if (x < bounds.getMinX() || x > bounds.getMaxX() || y < bounds.getMinY() || y > bounds.getMaxY())
         {
            return Double.NaN;
         }

         tmin = tmax = t;

      }
      else
      {
         final double invDirX = dir.getX() == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.getX());
         final double invDirY = dir.getY() == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.getY());
         final double invDirZ = dir.getZ() == 0.0 ? Double.POSITIVE_INFINITY : (1.0 / dir.getZ());
         final boolean signX = invDirX < 0.0;
         final boolean signY = invDirY < 0.0;
         final boolean signZ = invDirZ < 0.0;
         final double minX = bounds.getMinX();
         final double minY = bounds.getMinY();
         final double maxX = bounds.getMaxX();
         final double maxY = bounds.getMaxY();

         tmin = Double.NEGATIVE_INFINITY;
         tmax = Double.POSITIVE_INFINITY;
         if (Double.isInfinite(invDirX))
         {
            if (minX <= originX && maxX >= originX)
            {
               // move on, we are inside for the whole length
            }
            else
            {
               return Double.NaN;
            }
         }
         else
         {
            tmin = ((signX ? maxX : minX) - originX) * invDirX;
            tmax = ((signX ? minX : maxX) - originX) * invDirX;
         }

         if (Double.isInfinite(invDirY))
         {
            if (minY <= originY && maxY >= originY)
            {
               // move on, we are inside for the whole length
            }
            else
            {
               return Double.NaN;
            }
         }
         else
         {
            final double tymin = ((signY ? maxY : minY) - originY) * invDirY;
            final double tymax = ((signY ? minY : maxY) - originY) * invDirY;

            if ((tmin > tymax) || (tymin > tmax))
            {
               return Double.NaN;
            }
            if (tymin > tmin)
            {
               tmin = tymin;
            }
            if (tymax < tmax)
            {
               tmax = tymax;
            }
         }

         final double minZ = bounds.getMinZ();
         final double maxZ = bounds.getMaxZ();
         if (Double.isInfinite(invDirZ))
         {
            if (minZ <= originZ && maxZ >= originZ)
            {
               // move on, we are inside for the whole length
            }
            else
            {
               return Double.NaN;
            }
         }
         else
         {
            final double tzmin = ((signZ ? maxZ : minZ) - originZ) * invDirZ;
            final double tzmax = ((signZ ? minZ : maxZ) - originZ) * invDirZ;

            if ((tmin > tzmax) || (tzmin > tmax))
            {
               return Double.NaN;
            }
            if (tzmin > tmin)
            {
               tmin = tzmin;
            }
            if (tzmax < tmax)
            {
               tmax = tzmax;
            }
         }
      }

      // For clip we use following semantics: pick the node normally
      // if there is an intersection with the clip node. We don't consider
      // clip node distance.
      Node clip = node.getClip();
      if (clip != null
            // FIXME: All 3D picking is currently ignored by rendering.
            // Until this is fixed or defined differently (RT-28510),
            // we follow this behavior.
            && !(node instanceof Shape3D) && !(clip instanceof Shape3D))
      {
         final double dirX = dir.getX();
         final double dirY = dir.getY();
         final double dirZ = dir.getZ();

         Transform clipLocalToParentTransform = clip.getLocalToParentTransform();

         boolean hitClip = true;
         try
         {
            inverseTransform(clipLocalToParentTransform, origin);
            inverseTransform(clipLocalToParentTransform, dir);
         }
         catch (SingularMatrixException e)
         {
            hitClip = false;
         }
         hitClip = hitClip && intersects(clip, pickRay, null);
         origin.set(originX, originY, originZ);
         dir.set(dirX, dirY, dirZ);

         if (!hitClip)
         {
            return Double.NaN;
         }
      }

      if (Double.isInfinite(tmin) || Double.isNaN(tmin))
      {
         // We've got a nonsense pick ray or bounds.
         return Double.NaN;
      }

      final double minDistance = pickRay.nearClip;
      final double maxDistance = pickRay.farClip;
      if (tmin < minDistance)
      {
         if (tmax >= minDistance)
         {
            // we are inside bounds
            return 0.0;
         }
         else
         {
            return Double.NaN;
         }
      }
      else if (tmin > maxDistance)
      {
         return Double.NaN;
      }

      return tmin;
   }

   // Good to find a home for commonly use util. code such as EPS.
   // and almostZero. This code currently defined in multiple places,
   // such as Affine3D and GeneralTransform3D.
   private static final double EPSILON_ABSOLUTE = 1.0e-5;

   static boolean almostZero(double a)
   {
      return ((a < EPSILON_ABSOLUTE) && (a > -EPSILON_ABSOLUTE));
   }

   public static class CustomPickRay
   {
      public final Vector3D direction = new Vector3D();
      public final Point3D origin = new Point3D();
      public double nearClip, farClip;

      void applyTransform(Transform transform)
      {
         transform(transform, origin);
         transform(transform, direction);
      }
   }

   /**
    * Used during 3D picking process to determine the best pick result.
    */
   public static class CustomPickResultChooser
   {
      private double distance = Double.POSITIVE_INFINITY;
      private Node node;
      private int face = -1;
      private javafx.geometry.Point3D point;
      private javafx.geometry.Point3D normal;
      private javafx.geometry.Point2D texCoord;
      private boolean empty = true;
      private boolean closed = false;

      private boolean hasDepthBuffer;

      public CustomPickResultChooser()
      {
         this(true);
      }

      public CustomPickResultChooser(boolean hasDepthBuffer)
      {
         this.hasDepthBuffer = Platform.isSupported(ConditionalFeature.SCENE3D) && hasDepthBuffer;
      }

      /**
       * Helper method for computing intersected point. This method would fit better to PickRay but it
       * cannot work with Point3D (dependency issues).
       *
       * @param ray      Pick ray used for picking
       * @param distance Distance measured in ray direction magnitudes
       * @return the intersection point
       */
      public static javafx.geometry.Point3D computePoint(CustomPickRay ray, double distance)
      {
         Point3D origin = ray.origin;
         Vector3D dir = ray.direction;

         return new javafx.geometry.Point3D(origin.getX() + dir.getX() * distance,
                                            origin.getY() + dir.getY() * distance,
                                            origin.getZ() + dir.getZ() * distance);
      }

      /**
       * Converts the current content of this instance to the unmodifiable PickResult.
       * 
       * @return PickResult containing the current values of this chooser
       */
      public PickResult toPickResult()
      {
         if (empty)
         {
            return null;
         }
         return new PickResult(node, point, distance, face, normal, texCoord);
      }

      /**
       * Returns true if the given distance is smaller than the distance stored in this instance.
       * 
       * @param distance The distance to compare
       * @return true if the given distance is smaller
       */
      public boolean isCloser(double distance)
      {
         return distance < this.distance || empty;
      }

      /**
       * Returns true if there is no intersection stored in this instance.
       * 
       * @return true if there is no intersection stored in this instance.
       */
      public boolean isEmpty()
      {
         return empty;
      }

      /**
       * Returns true if this chooser has been closed. The chooser is closed when it is clear that no
       * further result can be accepted (due to disabled depth testing).
       * 
       * @return true if this chooser has been closed.
       * @see close()
       */
      public boolean isClosed()
      {
         return closed;
      }

      /**
       * Offers an intersection. If the given intersection is closer to the camera than the current one
       * (the distance is smaller), this instance is updated to hold the given values.
       * 
       * @param node     The intersected node
       * @param distance The intersected distance measured in pickRay direction magnitudes
       * @param face     The intersected face
       * @param point    The intersection point
       * @param texCoord The intersected texture coordinates
       * @return true if the offered intersection has been used
       */
      public boolean offer(Node node, double distance, int face, javafx.geometry.Point3D point, javafx.geometry.Point2D texCoord)
      {
         return processOffer(node, node, distance, point, face, normal, texCoord);
      }

      /**
       * Offers an intersection with a non-Shape3D object. This method is used for 2D objects and for 3D
       * objects with pickOnBounds==true; in both cases face and texCoord make no sense. If the given
       * intersection is closer to the camera than the current one (the distance is smaller), this
       * instance is updated to hold the given values.
       * 
       * @param node     The intersected node
       * @param distance The intersected distance measured in pickRay direction magnitudes
       * @param point    The intersection point
       * @return true if the offered intersection has been used
       */
      public boolean offer(Node node, double distance, javafx.geometry.Point3D point)
      {
         return processOffer(node, node, distance, point, PickResult.FACE_UNDEFINED, null, null);
      }

      /**
       * Process an offered intersection.
       * 
       * @see CustomPickResultChooser#offer(javafx.scene.Node, double, int, javafx.geometry.Point3D,
       *      javafx.geometry.Point2D)
       * @see CustomPickResultChooser#offer(javafx.scene.Node, double, javafx.geometry.Point3D)
       * @param node          The intersected node
       * @param depthTestNode The node whose depthTest is considered. When processing subScene pick result
       *                      we need to consider the inner picked node but subScene's depth test
       * @param distance      The intersected distance measured in pickRay direction magnitudes
       * @param point         The intersection point
       * @param face          The intersected face
       * @param normal        The intersected normal
       * @param texCoord      The intersected texture coordinates
       * @return true if the offered intersection has been used
       */
      private boolean processOffer(Node node,
                                   Node depthTestNode,
                                   double distance,
                                   javafx.geometry.Point3D point,
                                   int face,
                                   javafx.geometry.Point3D normal,
                                   javafx.geometry.Point2D texCoord)
      {
         final boolean hasDepthTest = hasDepthBuffer && isDerivedDepthTest(depthTestNode);

         boolean accepted = false;
         if ((empty || (hasDepthTest && distance < this.distance)) && !closed)
         {
            this.node = node;
            this.distance = distance;
            this.face = face;
            this.point = point;
            this.normal = normal;
            this.texCoord = texCoord;
            this.empty = false;
            accepted = true;
         }

         if (!hasDepthTest)
         {
            this.closed = true;
         }

         return accepted;
      }

      public static boolean isDerivedDepthTest(Node node)
      {
         DepthTest depthTest = node.getDepthTest();

         if (depthTest == DepthTest.INHERIT)
            return node.getParent() != null ? isDerivedDepthTest(node.getParent()) : true;
         else if (depthTest == DepthTest.ENABLE)
            return true;
         else
            return false;
      }
   }

   public static void transform(Transform transform, Point3DBasics pointToTransform)
   {
      transform(transform, pointToTransform, pointToTransform);
   }

   public static void transform(Transform transform, Point3DReadOnly pointOriginal, Tuple3DBasics pointTransformed)
   {
      transform(transform, pointOriginal, pointTransformed, true);
   }

   public static void transform(Transform transform, Vector3DBasics vectorToTransform)
   {
      transform(transform, vectorToTransform, vectorToTransform, false);
   }

   public static void transform(Transform transform, Vector3DReadOnly vectorOriginal, Tuple3DBasics vectorTransformed)
   {
      transform(transform, vectorOriginal, vectorTransformed, false);
   }

   public static void transform(Transform transform, Tuple3DReadOnly tupleOriginal, Tuple3DBasics tupleTransformed, boolean applyTranslation)
   {
      if (transform.isIdentity())
      {
         tupleTransformed.set(tupleOriginal);
      }
      else
      {
         double x_in = tupleOriginal.getX();
         double y_in = tupleOriginal.getY();
         double z_in = tupleOriginal.getZ();

         double x_out = transform.getMxx() * x_in + transform.getMxy() * y_in + transform.getMxz() * z_in;
         double y_out = transform.getMyx() * x_in + transform.getMyy() * y_in + transform.getMyz() * z_in;
         double z_out = transform.getMzx() * x_in + transform.getMzy() * y_in + transform.getMzz() * z_in;

         if (applyTranslation)
         {
            x_out += transform.getTx();
            y_out += transform.getTy();
            z_out += transform.getTz();
         }
         tupleTransformed.set(x_out, y_out, z_out);
      }
   }

   public static void inverseTransform(Transform transform, Point3DBasics pointToTransform)
   {
      inverseTransform(transform, pointToTransform, pointToTransform);
   }

   public static void inverseTransform(Transform transform, Point3DReadOnly pointOriginal, Tuple3DBasics pointTransformed)
   {
      inverseTransform(transform, pointOriginal, pointTransformed, true);
   }

   public static void inverseTransform(Transform transform, Vector3DBasics vectorToTransform)
   {
      inverseTransform(transform, vectorToTransform, vectorToTransform, false);
   }

   public static void inverseTransform(Transform transform, Vector3DReadOnly vectorOriginal, Tuple3DBasics vectorTransformed)
   {
      inverseTransform(transform, vectorOriginal, vectorTransformed, false);
   }

   public static void inverseTransform(Transform transform, Tuple3DReadOnly tupleOriginal, Tuple3DBasics tupleTransformed, boolean applyTranslation)
   {
      double x_in = tupleOriginal.getX();
      double y_in = tupleOriginal.getY();
      double z_in = tupleOriginal.getZ();
      double x_out, y_out, z_out;

      if (transform instanceof Rotate)
      {
         double m00 = transform.getMxx();
         double m11 = transform.getMyy();
         double m21 = transform.getMzy();
         double m20 = transform.getMzx();
         double m10 = transform.getMyx();
         double m01 = transform.getMxy();
         double m12 = transform.getMyz();
         double m22 = transform.getMzz();
         double m02 = transform.getMxz();

         x_out = m00 * x_in + m10 * y_in + m20 * z_in;
         y_out = m01 * x_in + m11 * y_in + m21 * z_in;
         z_out = m02 * x_in + m12 * y_in + m22 * z_in;
      }
      else if (transform instanceof Translate)
      {
         if (applyTranslation)
         {
            x_out = x_in - transform.getTx();
            y_out = y_in - transform.getTy();
            z_out = z_in - transform.getTz();
         }
         else
         {
            x_out = x_in;
            y_out = y_in;
            z_out = z_in;
         }
      }
      else if (transform instanceof Scale)
      {
         Scale scale = ((Scale) transform);
         x_out = x_in / scale.getX();
         y_out = y_in / scale.getY();
         z_out = z_in / scale.getZ();
      }
      else if (transform.isIdentity())
      {
         x_out = x_in;
         y_out = y_in;
         z_out = z_in;
      }
      else
      {
         double det = transform.determinant();
         if (det == 0.0) // To be consistent with JavaFX.
            throw new SingularMatrixException("Determinant is 0");

         double m00 = transform.getMxx();
         double m11 = transform.getMyy();
         double m21 = transform.getMzy();
         double m20 = transform.getMzx();
         double m10 = transform.getMyx();
         double m01 = transform.getMxy();
         double m12 = transform.getMyz();
         double m22 = transform.getMzz();
         double m02 = transform.getMxz();

         det = 1.0 / det;
         double invM00 = (m11 * m22 - m21 * m12) * det;
         double invM01 = -(m01 * m22 - m21 * m02) * det;
         double invM02 = (m01 * m12 - m11 * m02) * det;
         double invM10 = -(m10 * m22 - m20 * m12) * det;
         double invM11 = (m00 * m22 - m20 * m02) * det;
         double invM12 = -(m00 * m12 - m10 * m02) * det;
         double invM20 = (m10 * m21 - m20 * m11) * det;
         double invM21 = -(m00 * m21 - m20 * m01) * det;
         double invM22 = (m00 * m11 - m10 * m01) * det;

         double x_tem = x_in;
         double y_tem = y_in;
         double z_tem = z_in;

         if (applyTranslation)
         {
            x_tem = x_in - transform.getTx();
            y_tem = y_in - transform.getTy();
            z_tem = z_in - transform.getTz();
         }
         else
         {
            x_tem = x_in;
            y_tem = y_in;
            z_tem = z_in;
         }
         x_out = invM00 * x_tem + invM01 * y_tem + invM02 * z_tem;
         y_out = invM10 * x_tem + invM11 * y_tem + invM12 * z_tem;
         z_out = invM20 * x_tem + invM21 * y_tem + invM22 * z_tem;
      }

      tupleTransformed.set(x_out, y_out, z_out);
   }
}
