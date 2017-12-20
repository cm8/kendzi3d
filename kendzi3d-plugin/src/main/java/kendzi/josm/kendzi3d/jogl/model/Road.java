/*
 * This software is provided "AS IS" without a warranty of any kind. You use it
 * on your own risk and responsibility!!! This file is shared under BSD v3
 * license. See readme.txt and BSD3 file for details.
 */

package kendzi.josm.kendzi3d.jogl.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.jogamp.opengl.GL2;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import kendzi.jogl.camera.Camera;
import kendzi.jogl.model.factory.FaceFactory;
import kendzi.jogl.model.factory.FaceFactory.FaceType;
import kendzi.jogl.model.factory.MaterialFactory;
import kendzi.jogl.model.factory.MeshFactory;
import kendzi.jogl.model.factory.MeshFactoryUtil;
import kendzi.jogl.model.factory.ModelFactory;
import kendzi.jogl.model.geometry.Model;
import kendzi.jogl.model.geometry.TextCoord;
import kendzi.jogl.model.geometry.material.Material;
import kendzi.jogl.model.render.ModelRender;
import kendzi.josm.kendzi3d.jogl.model.export.ExportItem;
import kendzi.josm.kendzi3d.jogl.model.export.ExportModelConf;
import kendzi.josm.kendzi3d.jogl.model.tmp.AbstractWayModel;
import kendzi.josm.kendzi3d.service.MetadataCacheService;
import kendzi.josm.kendzi3d.util.ModelUtil;
import kendzi.kendzi3d.josm.model.perspective.Perspective;
import kendzi.kendzi3d.josm.model.polygon.PolygonWithHolesUtil;
import kendzi.math.geometry.Plane3d;
import kendzi.math.geometry.Triangle2d;
import kendzi.math.geometry.polygon.PolygonWithHolesList2d;
import kendzi.math.geometry.triangulate.Poly2TriSimpleUtil;

import org.apache.log4j.Logger;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Represent road.
 *
 * This class require lot of clean up!
 *
 * @author Tomasz Kedziora (Kendzi)
 *
 */
@Deprecated
public class Road extends AbstractWayModel {

    /** Log. */
    private static final Logger log = Logger.getLogger(Road.class);

    /**
     * Default width of road.
     */
    private static final double DEFAULT_ROAD_WIDTH = 6.0f;

    /**
     * Renderer of model.
     */
    private final ModelRender modelRender;

    /**
     * Metadata cache service.
     */
    private final MetadataCacheService metadataCacheService;

    /**
     * Texture data.
     */
    private TextureData textureData;

    /**
     * List of road points.
     */
    private List<Point2d> list = new ArrayList<Point2d>();

    /**
     * Width of road.
     */
    private double roadWidth;

    /**
     * Sin of 90.
     */
    private static double cos90 = Math.cos(Math.toRadians(90));

    /**
     * Cos of 90.
     */
    private static double sin90 = Math.sin(Math.toRadians(90));

    /**
     * Model of road.
     */
    private Model model;

    private Relation mp;

    /**
     * Represent road.
     *
     * @param way
     *            way
     * @param pPerspective
     *            perspective
     * @param pModelRender
     *            model render
     * @param pMetadataCacheService
     *            metadata cache service
     */
    public Road(Way way, Perspective pPerspective, ModelRender pModelRender, MetadataCacheService pMetadataCacheService) {

        super(way, pPerspective);

        modelRender = pModelRender;
        metadataCacheService = pMetadataCacheService;
        textureData = getTexture();
    }

    public Road(Relation relation, Perspective pPerspective, ModelRender pModelRender, MetadataCacheService pMetadataCacheService) {

        super(null, pPerspective);
        mp = relation;

        modelRender = pModelRender;
        metadataCacheService = pMetadataCacheService;
        textureData = getTexture();
    }

    @Override
    public void buildWorldObject() {

        // FIXME object is not in local coordinates!
        setPoint(new Point3d());

        if (mp != null || way.hasAreaTags()) {
            model = buildArea();
        } else {
            model = buildLinear();
        }

        model.setUseLight(true);
        model.setUseTexture(true);
        buildModel = true;
    }

    private int getMaterial(ModelFactory mf) {

        Material m = MaterialFactory.createTextureMaterial(textureData.getFile());

        return mf.addMaterial(m);
    }

    private Model buildArea() {

        ModelFactory modelBuilder = ModelFactory.modelBuilder();
        MeshFactory mesh = modelBuilder.addMesh("road_area");

        mesh.materialID = getMaterial(modelBuilder);
        mesh.hasTexture = true;

        Vector3d nt = new Vector3d(0, 1, 0);

        Point3d planeRightTopPoint = new Point3d(0, 0.05, 0);

        List<PolygonWithHolesList2d> polyList = PolygonWithHolesUtil.getMultiPolygonWithHoles(way != null ? way : mp, perspective);

        for (PolygonWithHolesList2d poly : polyList) {

            List<Triangle2d> triangles = Poly2TriSimpleUtil.triangulate(poly);

            Plane3d planeTop = new Plane3d(planeRightTopPoint, nt);

            Vector3d roofTopLineVector = new Vector3d(-1, 0, 0);

            MeshFactoryUtil.addPolygonToRoofMesh(mesh, triangles, planeTop, roofTopLineVector,
                    new kendzi.jogl.texture.dto.TextureData(textureData.getFile(), 1d, 1d),
                    0d, 0d);
        }

        return modelBuilder.toModel();
    }

    private void buildStrip(MeshFactory mf, FaceFactory ff, Point3d spOut, Point3d spIn, Point3d epOut, Point3d epIn,
            double uStart, double uEnd, double vOut, double vIn, int nIdx) {

        // outward border
        int tcb1 = mf.addTextCoord(new TextCoord(uStart, vOut));
        // inward border
        int tcb2 = mf.addTextCoord(new TextCoord(uStart, vIn));

        // outward border
        int tce1 = mf.addTextCoord(new TextCoord(uEnd, vOut));
        // inward border
        int tce2 = mf.addTextCoord(new TextCoord(uEnd, vIn));

        // outward border start vertex
        int wbi1 = mf.addVertex(new Point3d(spOut));
        // inward border start vertex
        int wbi2 = mf.addVertex(new Point3d(spIn));

        // outward border end vertex
        int wei1 = mf.addVertex(new Point3d(epOut));
        // inward border end vertex
        int wei2 = mf.addVertex(new Point3d(epIn));

        ff.addVert(wbi1, tcb1, nIdx);
        ff.addVert(wbi2, tcb2, nIdx);
        ff.addVert(wei1, tce1, nIdx);
        ff.addVert(wei2, tce2, nIdx);
    }

    private Model buildLinear() {

        boolean highway_links_join = false;
        int oneway = way.isOneway();
        if (oneway != 0) {
            final List<OsmPrimitive> links = new ArrayList<>();

            Node join_at = Arrays.asList(way.firstNode(), way.lastNode()).stream()
                    .filter(Objects::nonNull)
                    .map(n -> {
                        List<OsmPrimitive> refs = n.getReferrers().stream()
                                .filter(t -> t instanceof Way && t != way)
                                .collect(Collectors.toList());
                        refs.stream().filter(t -> ((Way)t).isOneway() != 0).forEach(e -> links.add(e));
                        if (links.size() == 1 && ((Way) links.get(0)).isFirstLastNode(n)
                                && refs.size() - links.size() > 0) {
                            return n;
                        } else {
                            return null;
                        }
                    }).filter(Objects::nonNull).findFirst().orElse(null);

            if (join_at != null) {
                highway_links_join = true;
                oneway = (way.lastNode() == join_at) ? 1 : -1;
                oneway *= det(join_at, way.getNeighbours(join_at).iterator().next(),
                        ((Way) links.get(0)).getNeighbours(join_at).iterator().next()) > 0 ? 1 : -1;
            }
        }

        List<Point2d> pointList = new ArrayList<Point2d>();

        for (int i = 0; i < way.getNodesCount(); i++) {
            Node node = way.getNode(i);
            pointList.add(perspective.calcPoint(node));
        }

        list = pointList;

        roadWidth = getRoadWidth();

        ModelFactory modelBuilder = ModelFactory.modelBuilder();

        MeshFactory meshWalls = modelBuilder.addMesh("road");

        meshWalls.materialID = getMaterial(modelBuilder);
        meshWalls.hasTexture = true;

        if (list.size() > 1) {

            Vector3d flatSurface = new Vector3d(0, 1, 0);

            int flatNormalI = meshWalls.addNormal(flatSurface);

            final List<Double> xbs = new ArrayList<>();
            final List<Double> zbs = new ArrayList<>();
            final Point2d offb = new Point2d(0d, 0d);
            double orthX = 0;
            double orthY = 0;
            Double prevAngle = null;

            for (int i = 1; i < list.size(); i++) {
                final Point2d beginPoint = list.get(i - 1);
                final Point2d endPoint = list.get(i);

                double x = endPoint.x - beginPoint.x;
                double y = endPoint.y - beginPoint.y;
                // calc lenght of road segment
                double mod = Math.sqrt(x * x + y * y);

                double distance = beginPoint.distance(endPoint);

                // calc orthogonal for road segment
                orthX = x * cos90 + y * sin90;
                orthY = -x * sin90 + y * cos90;

                // calc vector for road width;
                double normX = roadWidth / 2 * orthX / mod;
                double normY = roadWidth / 2 * orthY / mod;
                // calc vector for border width;
                double borderX = normX + 0.2 * orthX / mod;
                double borderY = normY + 0.2 * orthY / mod;

                double uEnd = distance / textureData.getLenght();

                final Point2d off = new Point2d(0d, 0d);
                if (highway_links_join) {
                    off.x = oneway * (normX + borderX) / 2;
                    off.y = oneway * (normY + borderY) / 2;
                }

                double vStart = 0.00001d;

                if (getKV("lanes") != null && getKV("lanes").equals("lanes_1")) {
                    vStart += 0.5;
                }

                // clamp Points of adjacent segments of this way for smoother appearance
                // FIXME: should use proper miter join algorithm to maintain exact width
                double angle = Math.atan2(orthX, orthY);
                if (angle > Math.PI) {
                    angle -= 2*Math.PI;
                }
                if (angle <= -Math.PI) {
                    angle += 2*Math.PI;
                }
                if (prevAngle != null && Math.abs(angle-prevAngle) > Math.PI / 6d) {
                    xbs.clear();
                    zbs.clear();
                }
                prevAngle = angle;

                List<Double> tv = Arrays.asList(0.99999d, 1 - 0.10d, vStart);
                List<Double> xs = Arrays.asList(borderX, normX, 0d);
                List<Double> ys = Arrays.asList(0d, 0.1d, 0.15d);
                List<Double> zs = Arrays.asList(borderY, normY, 0d);
                HashMap<Integer, FaceFactory> ff = new HashMap<>();

                IntStream.concat(IntStream.range(0, tv.size()-1), IntStream.iterate(tv.size()-1, t -> t-1).limit(tv.size()))
                .reduce(-1, (r, e) -> {
                    if (r >= 0) {
                        int s = r - e;
                        Point3d spOutward = new Point3d(
                                +(beginPoint.x + s * (xbs.isEmpty() ? xs : xbs).get(r) + (xbs.isEmpty() ? off : offb).x), ys.get(r),
                                -(beginPoint.y + s * (zbs.isEmpty() ? zs : zbs).get(r) + (zbs.isEmpty() ? off : offb).y));
                        Point3d spInward = new Point3d(
                                +(beginPoint.x + s * (xbs.isEmpty() ? xs : xbs).get(e) + (xbs.isEmpty() ? off : offb).x), ys.get(e),
                                -(beginPoint.y + s * (zbs.isEmpty() ? zs : zbs).get(e) + (zbs.isEmpty() ? off : offb).y));
                        Point3d epOutward = new Point3d(
                                +(endPoint.x + s * xs.get(r) + off.x), ys.get(r),
                                -(endPoint.y + s * zs.get(r) + off.y));
                        Point3d epInward = new Point3d(
                                +(endPoint.x + s * xs.get(e) + off.x), ys.get(e),
                                -(endPoint.y + s * zs.get(e) + off.y));

                        buildStrip(meshWalls, ff.compute(s < 0 ? 0x1000 + e : e,
                                (k, v) -> (v == null) ? meshWalls.addFace(FaceType.QUAD_STRIP) : v),
                                spOutward, spInward, epOutward, epInward, 0, uEnd, tv.get(r), tv.get(e), flatNormalI);
                    }
                    return e;
                });

                xbs.clear();
                xbs.addAll(xs);
                zbs.clear();
                zbs.addAll(zs);
                offb.set(off.x, off.y);
            }
        }

        return modelBuilder.toModel();
    }

    private String getKV(String k) {

        String v;

        if (k.equals("area")) {
            v = (mp != null || way.hasAreaTags()) ? "yes" : null;
        } else if (k.equals("oneway")) {
            v = (mp == null && way.isOneway() != 0) ? "yes" : null;
        } else {
            v = (mp != null ? mp : way).get(k);
        }

        return (v == null) ? null : k + "_" + v;
    }

    /**
     * Finds texture data.
     *
     * @return texture data
     */
    private TextureData getTexture() {

        List<String> keys = Arrays.asList("highway", "surface", "lanes", "area")
                .stream().map(k -> getKV(k)).filter(Objects::nonNull).collect(Collectors.toList());

        List<List<String>> propkeys = keys
                .stream().collect(LinkedList<List<String>>::new,
                        (r, kv) -> {
                            List<List<String>> c = new LinkedList<>();
                            r.forEach(ll -> c.add(0, ll));
                            c.add(new LinkedList<>());
                            c.stream().forEach(ll -> {
                                final int depth = ll.size() + 1;
                                int ni = ll.size(); // assign 0 to generate all possible orderings
                                int ri = (int) r.stream().filter(t -> t.size() > depth).count();
                                for (; ni < depth; ni++, ri++) {   // t.size()<= depth (ascending)
                                    List<String> n = new LinkedList<>(ll);
                                    n.add(ni, kv);
                                    r.add(ri, n);
                                }
                            });
                        }, (r1, r2) -> r1.addAll(r2))
                .stream().collect(Collectors.toList());

        //propkeys.stream().forEach(pk -> System.out.println(String.join(".", pk)));

        SimpleEntry<String, String> propname = IntStream.range(0, keys.size())
                .mapToObj(tail -> keys.subList(keys.size() - tail, keys.size()))
                .flatMap(cnv -> propkeys.stream().map(pk -> {
                    cnv.forEach(kv -> {
                        if (pk.indexOf(kv) >= 0) {
                            pk.set(pk.indexOf(kv), kv.replaceFirst("_.*", "_unknown"));
                        }
                    });
                    pk.add(0, "roads");
                    String p = String.join(".", pk);
                    String tex = metadataCacheService.getPropertites(p + ".texture.file", null);
                    if (tex != null) {
                        return new SimpleEntry<>(p, tex);
                    }
                    return null;
                }))
                .filter(Objects::nonNull)
                .findFirst().orElse(new SimpleEntry<>("", null));

        Double length = metadataCacheService.getPropertitesDouble(propname.getKey() + ".texture.lenght", 1d);
        Double width = metadataCacheService.getPropertitesDouble(propname.getKey() + ".width", DEFAULT_ROAD_WIDTH);

        if (!propname.getKey().contains("lanes_")) {
            width = keys.stream().filter(t -> t.startsWith("lanes_"))
                    .map(t -> Integer.parseInt(t.split("_")[1]))
                    .filter(t -> t > 0).findFirst().orElse(getKV("oneway") == null ? 2 : 1)
                    * width / 2;
        }

        return new TextureData(propname.getValue(), length, width);
    }

    /**
     * Texture data.
     *
     * @author kendzi
     *
     */
    private class TextureData {
        final String file;
        final double lenght;
        final double width;

        private TextureData(String pFile, double pLenght, double pWidth) {
            super();
            file = pFile;
            lenght = pLenght;
            width = pWidth;
        }

        /**
         * @return the file
         */
        public String getFile() {
            return file;
        }

        /**
         * @return the lenght
         */
        public double getLenght() {
            return lenght;
        }

        /**
         * @return the width
         */
        public double getWidth() {
            return width;
        }
    }

    /**
     * @param n1 start node of vector v
     * @param n2 end node of vector v
     * @param n3 other node
     * @return negative value if n3 lies right to the directed line given by v
     *         positive value if n3 lies left, and zero if n3 lies on the line
     *
     * | n1.lat - n3.lat   n1.lon - n3.lon |      n3? (.. positive result)
     * | n2.lat - n3.lat   n2.lon - n3.lon |  n1 -----> n2
     *                                         n3?    (.. negative result)
     */
    private double det(Node n1, Node n2, Node n3) {
        return ((n1.lat() - n3.lat()) * (n2.lon() - n3.lon()) -
                (n1.lon() - n3.lon()) * (n2.lat() - n3.lat()));
    }

    /**
     * Finds road width.
     *
     * @return road width
     */
    private double getRoadWidth() {

        try {
            return ModelUtil.parseHeight(way.get("width"), textureData.getWidth());
        } catch (Exception e) {
            log.error(e, e);
        }

        return DEFAULT_ROAD_WIDTH;
    }

    @Override
    public void draw(GL2 gl, Camera camera, boolean selected) {
        draw(gl, camera);
    }

    @Override
    public void draw(GL2 pGl, Camera pCamera) {
        if (mp == null) {
            // FIXME object is not in local coordinates!
            modelRender.render(pGl, model);
        }

        pGl.glPushMatrix();
        pGl.glTranslated(getGlobalX(), 0, -getGlobalY());

        try {
            modelRender.render(pGl, model);

        } finally {
            pGl.glPopMatrix();
        }
    }

    @Override
    public List<ExportItem> export(ExportModelConf conf) {
        if (model == null) {
            buildWorldObject();
        }

        return Collections
                .singletonList(new ExportItem(model, new Point3d(getGlobalX(), 0, -getGlobalY()), new Vector3d(1, 1, 1)));
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public Point3d getPosition() {
        return getPoint();
    }

    @Override
    public void rebuildWorldObject(OsmPrimitive primitive, Perspective perspective) {
        if (primitive instanceof Relation) {
            mp = (Relation) primitive;
            primitive = null;
        }
        textureData = getTexture();
        super.rebuildWorldObject(primitive, perspective);
    }
}
