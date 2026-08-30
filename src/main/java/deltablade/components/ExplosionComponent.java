package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Component that plays an explosion animation from a sprite sheet.
 * Supports both horizontal strips (1 row) and grid layouts (multiple rows).
 * For grids, frames are read row-major: left-to-right, then top-to-bottom.
 * After the last frame, the entity is removed from the world.
 */
public class ExplosionComponent extends Component {

    private final ImageView imageView;
    private final int frameCount;
    private final int frameWidth;
    private final int frameHeight;
    private final int columns;
    private final double frameDuration;

    private double elapsed = 0;
    private int currentFrame = 0;
    private boolean finished = false;

    /**
     * Creates an explosion animation component.
     *
     * @param spriteSheet the sprite sheet image (strip or grid)
     * @param frameCount  total number of frames in the sheet
     * @param frameWidth  width of each frame in pixels
     * @param frameHeight height of each frame in pixels
     * @param columns     number of columns in the grid (for strips, this equals frameCount)
     * @param totalDuration total animation duration in seconds
     */
    public ExplosionComponent(Image spriteSheet, int frameCount, int frameWidth, int frameHeight, int columns, double totalDuration) {
        this.frameCount = frameCount;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.columns = columns;
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
            int col = currentFrame % columns;
            int row = currentFrame / columns;
            imageView.setViewport(new Rectangle2D(col * frameWidth, row * frameHeight, frameWidth, frameHeight));
        }
    }
}
