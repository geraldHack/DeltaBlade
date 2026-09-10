package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;

/**
 * Component for scrolling stars with vertical wrapping and optional twinkle effect.
 * During warp, stars stretch into streaks and accelerate.
 */
public class StarComponent extends Component {

    private static double warp = 0;

    private final double scrollSpeed;
    private final double baseOpacity;
    private final Circle circle;
    private final Rectangle streak;
    
    private double twinklePhase;
    private final double twinkleSpeed;
    private final double twinkleAmount;

    public static void setWarp(double intensity) {
        warp = Math.max(0, Math.min(1, intensity));
    }

    public StarComponent(Circle circle, double scrollSpeed, double baseOpacity, double twinkleSpeed, double twinkleAmount) {
        this.circle = circle;
        this.scrollSpeed = scrollSpeed;
        this.baseOpacity = baseOpacity;
        this.twinkleSpeed = twinkleSpeed;
        this.twinkleAmount = twinkleAmount;
        this.twinklePhase = Math.random() * Math.PI * 2;
        this.streak = new Rectangle(Math.max(1.2, circle.getRadius() * 1.4), 1);
        this.streak.setArcWidth(1);
        this.streak.setArcHeight(1);
        this.streak.setFill(Color.rgb(210, 230, 255));
        this.streak.setOpacity(0);
        this.streak.setMouseTransparent(true);
    }

    @Override
    public void onAdded() {
        entity.getViewComponent().addChild(streak);
    }

    @Override
    public void onUpdate(double tpf) {
        double speed = scrollSpeed * (1 + warp * 16);
        entity.translateY(speed * tpf);
        
        if (entity.getY() > getAppHeight() + 40) {
            entity.setY(-40);
        }
        
        if (twinkleAmount > 0 && warp < 0.2) {
            twinklePhase += twinkleSpeed * tpf;
            double opacityMod = Math.sin(twinklePhase) * twinkleAmount;
            circle.setOpacity(Math.max(0.1, Math.min(1.0, baseOpacity + opacityMod)));
        }

        if (warp > 0.02) {
            double h = 4 + warp * (22 + scrollSpeed * 0.55);
            streak.setHeight(h);
            streak.setTranslateX(-streak.getWidth() / 2);
            streak.setTranslateY(-h / 2);
            streak.setOpacity(Math.min(1.0, baseOpacity * (0.35 + warp * 0.9)));
            circle.setOpacity(baseOpacity * (1 - warp * 0.75));
        } else {
            streak.setOpacity(0);
            streak.setHeight(1);
        }
    }
}
