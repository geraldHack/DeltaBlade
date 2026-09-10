package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.image.ImageView;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;

/**
 * Vertically wraps two stacked tiles of a seamless space background.
 */
public class BackgroundScrollComponent extends Component {

    private static double warp = 0;

    private final ImageView tileA;
    private final ImageView tileB;
    private final double tileHeight;
    private final double scrollSpeed;

    public static void setWarp(double intensity) {
        warp = Math.max(0, Math.min(1, intensity));
    }

    public BackgroundScrollComponent(ImageView tileA, ImageView tileB, double tileHeight, double scrollSpeed) {
        this.tileA = tileA;
        this.tileB = tileB;
        this.tileHeight = tileHeight;
        this.scrollSpeed = scrollSpeed;
    }

    @Override
    public void onUpdate(double tpf) {
        double dy = scrollSpeed * (1 + warp * 14) * tpf;
        wrap(tileA, dy);
        wrap(tileB, dy);
    }

    private void wrap(ImageView tile, double dy) {
        double y = tile.getTranslateY() + dy;
        double limit = getAppHeight();
        if (y >= limit) {
            y -= tileHeight * 2;
        }
        tile.setTranslateY(y);
    }
}
