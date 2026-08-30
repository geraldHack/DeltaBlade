package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Animated player ship from Gerald's blue-player sheets.
 * Each sheet is 384x48 (8 frames of 48x48, 120ms).
 *
 * Düsenausstoß: looping idle thruster
 * Seitliches Krängen: frames 1-3 bank right, 5-7 bank left (both directions in one sheet)
 * Bugwelle: nose cone, reserved for forward thrust
 * Strahl-Puls: muzzle flash while firing
 */
public class PlayerAnimationComponent extends Component {

    public enum AnimationState {
        IDLE,
        BANKING_LEFT,
        BANKING_RIGHT,
        MOVING_UP,
        FIRING
    }

    private final ImageView imageView;
    private final Image thrusterSheet;
    private final Image bankSheet;
    private final Image bowwaveSheet;
    private final Image beamSheet;

    private static final int FRAME_COUNT = 8;
    private static final int FRAME_WIDTH = 48;
    private static final int FRAME_HEIGHT = 48;
    private static final double FRAME_DURATION = 0.120;

    private AnimationState currentState = AnimationState.IDLE;
    private double elapsed = 0;
    private int currentFrame = 0;
    private int frameMin = 0;
    private int frameMax = FRAME_COUNT - 1;
    private boolean holdLast = false;

    private Image currentSheet;

    public PlayerAnimationComponent(Image thrusterSheet, Image bankSheet, Image bowwaveSheet) {
        this(thrusterSheet, bankSheet, bowwaveSheet, null);
    }

    public PlayerAnimationComponent(Image thrusterSheet, Image bankSheet, Image bowwaveSheet, Image beamSheet) {
        this.thrusterSheet = thrusterSheet;
        this.bankSheet = bankSheet;
        this.bowwaveSheet = bowwaveSheet;
        this.beamSheet = beamSheet;
        this.currentSheet = thrusterSheet;

        this.imageView = new ImageView(thrusterSheet);
        this.imageView.setFitWidth(FRAME_WIDTH);
        this.imageView.setFitHeight(FRAME_HEIGHT);
        this.imageView.setSmooth(false);
        this.imageView.setPreserveRatio(false);
        this.imageView.setViewport(new Rectangle2D(0, 0, FRAME_WIDTH, FRAME_HEIGHT));
        applyRange(AnimationState.IDLE);
    }

    public ImageView getView() {
        return imageView;
    }

    public void setState(AnimationState newState) {
        if (currentState == newState) {
            return;
        }
        currentState = newState;
        Image newSheet = getSheetForState(newState);
        if (newSheet != currentSheet && newSheet != null) {
            currentSheet = newSheet;
            imageView.setImage(currentSheet);
        }
        applyRange(newState);
        elapsed = 0;
        currentFrame = frameMin;
        imageView.setViewport(new Rectangle2D(currentFrame * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT));
    }

    public AnimationState getState() {
        return currentState;
    }

    private void applyRange(AnimationState state) {
        holdLast = false;
        switch (state) {
            case BANKING_RIGHT -> {
                frameMin = 1;
                frameMax = 3;
                holdLast = true;
            }
            case BANKING_LEFT -> {
                frameMin = 5;
                frameMax = 7;
                holdLast = true;
            }
            default -> {
                frameMin = 0;
                frameMax = FRAME_COUNT - 1;
            }
        }
    }

    private Image getSheetForState(AnimationState state) {
        return switch (state) {
            case BANKING_LEFT, BANKING_RIGHT -> bankSheet != null ? bankSheet : thrusterSheet;
            case MOVING_UP -> bowwaveSheet != null ? bowwaveSheet : thrusterSheet;
            case FIRING -> beamSheet != null ? beamSheet : thrusterSheet;
            default -> thrusterSheet;
        };
    }

    @Override
    public void onUpdate(double tpf) {
        elapsed += tpf;

        int span = frameMax - frameMin + 1;
        int steps = (int) (elapsed / FRAME_DURATION);
        int newFrame;
        if (holdLast) {
            newFrame = frameMin + Math.min(steps, span - 1);
        } else {
            newFrame = frameMin + (steps % span);
        }

        if (newFrame != currentFrame) {
            currentFrame = newFrame;
            imageView.setViewport(new Rectangle2D(currentFrame * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT));
        }
    }

    public int getFrameWidth() {
        return FRAME_WIDTH;
    }

    public int getFrameHeight() {
        return FRAME_HEIGHT;
    }
}
