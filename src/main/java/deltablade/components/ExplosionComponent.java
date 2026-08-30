package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Component that plays an explosion animation from a sprite sheet.
 * Supports both single-row strips and grid-based sheets.
 * Uses col = i % columns, row = i / columns for grid playback.
 * After the last frame, the entity is removed from the world.
 */
public class ExplosionComponent extends Component {

    private final ImageView imageView;
    private final int frameCount;
    private final int frameWidth;
    private final int frameHeight;
    private final int columns;
    private final double frameDuration;
    private final boolean isFallback;

    private double elapsed = 0;
    private int currentFrame = 0;
    private boolean finished = false;

    /**
     * Creates an explosion animation component with grid-based playback and custom display size.
     *
     * @param spriteSheet the sprite sheet image
     * @param frameCount  number of frames in the sheet
     * @param frameWidth  width of each frame in pixels
     * @param frameHeight height of each frame in pixels
     * @param columns     number of columns in the grid
     * @param totalDuration total animation duration in seconds
     * @param displayWidth  display width for the view
     * @param displayHeight display height for the view
     */
    public ExplosionComponent(Image spriteSheet, int frameCount, int frameWidth, int frameHeight, 
                              int columns, double totalDuration, int displayWidth, int displayHeight) {
        this.frameCount = frameCount;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.columns = columns;
        this.frameDuration = totalDuration / frameCount;
        this.isFallback = false;

        this.imageView = new ImageView(spriteSheet);
        this.imageView.setFitWidth(displayWidth);
        this.imageView.setFitHeight(displayHeight);
        this.imageView.setSmooth(false);
        this.imageView.setPreserveRatio(false);
        this.imageView.setViewport(new Rectangle2D(0, 0, frameWidth, frameHeight));
    }

    /**
     * Creates a fallback explosion component (no sprite sheet, just timing).
     * Used when sprite decoding fails.
     *
     * @param totalDuration total animation duration in seconds
     */
    public ExplosionComponent(double totalDuration) {
        this.frameCount = 1;
        this.frameWidth = 0;
        this.frameHeight = 0;
        this.columns = 1;
        this.frameDuration = totalDuration;
        this.isFallback = true;
        this.imageView = null;
    }

    /**
     * Returns the ImageView to be used as the entity's view.
     * May return null for fallback explosions.
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

        if (isFallback) {
            if (elapsed >= frameDuration) {
                finished = true;
                entity.removeFromWorld();
            }
            return;
        }

        int newFrame = (int) (elapsed / frameDuration);

        if (newFrame >= frameCount) {
            finished = true;
            entity.removeFromWorld();
            return;
        }

        if (newFrame != currentFrame) {
            currentFrame = newFrame;
            int col = currentFrame % columns;
            int row = currentFrame / columns;
            imageView.setViewport(new Rectangle2D(col * frameWidth, row * frameHeight, frameWidth, frameHeight));
        }
    }
}
