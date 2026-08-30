package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Component that plays an explosion animation from a horizontal sprite strip.
 * The strip should contain N frames of WxH pixels each.
 * After the last frame, the entity is removed from the world.
 */
public class ExplosionComponent extends Component {

    private final ImageView imageView;
    private final int frameCount;
    private final int frameWidth;
    private final int frameHeight;
    private final double frameDuration;

    private double elapsed = 0;
    private int currentFrame = 0;
    private boolean finished = false;

    /**
     * Creates an explosion animation component.
     *
     * @param spriteSheet the horizontal sprite strip image
     * @param frameCount  number of frames in the strip
     * @param frameWidth  width of each frame in pixels
     * @param frameHeight height of each frame in pixels
     * @param totalDuration total animation duration in seconds
     */
    public ExplosionComponent(Image spriteSheet, int frameCount, int frameWidth, int frameHeight, double totalDuration) {
        this.frameCount = frameCount;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameDuration = totalDuration / frameCount;

        this.imageView = new ImageView(spriteSheet);
        this.imageView.setFitWidth(frameWidth);
        this.imageView.setFitHeight(frameHeight);
        this.imageView.setSmooth(false);
        this.imageView.setPreserveRatio(false);
        this.imageView.setViewport(new Rectangle2D(0, 0, frameWidth, frameHeight));
    }

    /**
     * Returns the ImageView to be used as the entity's view.
     */
    public ImageView getView() {
        return imageView;
    }

    @Override
    public void onUpdate(double tpf) {
        if (finished) {
            return;
        }

        elapsed += tpf;

        int newFrame = (int) (elapsed / frameDuration);

        if (newFrame >= frameCount) {
            finished = true;
            entity.removeFromWorld();
            return;
        }

        if (newFrame != currentFrame) {
            currentFrame = newFrame;
            imageView.setViewport(new Rectangle2D(currentFrame * frameWidth, 0, frameWidth, frameHeight));
        }
    }
}
