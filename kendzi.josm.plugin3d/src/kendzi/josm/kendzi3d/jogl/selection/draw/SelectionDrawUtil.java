package kendzi.josm.kendzi3d.jogl.selection.draw;

import java.awt.Color;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import kendzi.jogl.DrawUtil;
import kendzi.josm.kendzi3d.jogl.selection.ObjectSelectionManager;
import kendzi.josm.kendzi3d.jogl.selection.Selection;
import kendzi.josm.kendzi3d.jogl.selection.editor.ArrowEditor;
import kendzi.josm.kendzi3d.jogl.selection.editor.Editor;
import kendzi.math.geometry.ray.Ray3d;

public class SelectionDrawUtil {

    private static float[] colorArrays = new float[4];

    private GLUquadric quadratic;   // Storage For Our Quadratic Objects
    private GLU glu = new GLU();

    public void init(GL2 gl) {
        this.quadratic = this.glu.gluNewQuadric();
        this.glu.gluQuadricNormals(this.quadratic, GLU.GLU_SMOOTH); // Create Smooth Normals
    }

    public void draw(GL2 gl, ObjectSelectionManager manager) {

        if (manager.getLastClosestPointOnBaseRay() != null) {
            drawPoint(gl, manager.getLastClosestPointOnBaseRay());
        }

        Ray3d select = manager.getLastSelectRay();
        if (select != null) {
            drawSelectRay(gl, select);
        }

        Selection selection = manager.getLastSelection();
        if (selection != null) {
            drawEditors(gl, selection);
        }

    }

    private void drawEditors(GL2 gl, Selection selection) {
        List<Editor> editors = selection.getEditors();

        gl.glDisable(GL2.GL_TEXTURE_2D);
       // gl.glEnable(GL2.GL_LIGHTING);

        for (Editor editor : editors) {
            if (editor instanceof ArrowEditor) {
                ArrowEditor ae = (ArrowEditor) editor;
                Point3d p = ae.getPoint();
                Vector3d v = ae.getVector();
                double l = ae.getLength();

                if (ae.isSelect()) {
                    gl.glColor3fv(Color.RED.darker().darker().darker().getRGBComponents(new float[4]), 0);
                } else {
                    gl.glColor3fv(Color.green.darker().darker().darker().getRGBComponents(new float[4]), 0);
                }

                gl.glPushMatrix();
                gl.glTranslated(p.x, p.y, p.z);

//                DrawUtil.drawDotY(gl, 0.3, 6);
                this.glu.gluSphere(this.quadratic, ObjectSelectionManager.SELECTION_ETITOR_RADIUS, 32, 32);

                gl.glPopMatrix();


                gl.glPushMatrix();
                gl.glTranslated(p.x + v.x * l, p.y + v.y * l, p.z + v.z * l);

               // DrawUtil.drawDotY(gl, 0.3, 6);
             // Draw A Sphere With A Radius Of 1 And 16 Longitude And 16 Latitude Segments
                this.glu.gluSphere(this.quadratic, 0.3f, 32, 32);
//                http://www.felixgers.de/teaching/jogl/gluQuadricPrimitives.html
             // A Cylinder With A Radius Of 0.5 And A Height Of 2
//                glu.gluCylinder(quadratic, 1.0f, 1.0f, 3.0f, 32, 32);

                gl.glPopMatrix();
            }
        }
    }

    /**
     * @param gl
     * @param select
     */
    public static void drawSelectRay(GL2 gl, Ray3d select) {
        gl.glPushMatrix();

        Vector3d v = select.getVector();
        Point3d p = select.getPoint();

        double dx = p.x + 10*v.x;
        double dy = p.y + 10*v.y;
        double dz = p.z + 10*v.z;


        gl.glTranslated(dx, dy, dz);

        gl.glColor3fv(Color.ORANGE.darker().getRGBComponents(colorArrays), 0);

        DrawUtil.drawDotY(gl, 0.3, 6);

        gl.glPopMatrix();
    }


    /**
     * @param gl
     * @param select
     */
    public static void drawPoint(GL2 gl, Point3d p) {
        gl.glPushMatrix();

        double dx = p.x;
        double dy = p.y;
        double dz = p.z;

        gl.glTranslated(dx, dy, dz);

        gl.glColor3fv(Color.ORANGE.darker().getRGBComponents(new float[4]), 0);

        DrawUtil.drawDotY(gl, 0.6, 6);

        gl.glPopMatrix();
    }

}