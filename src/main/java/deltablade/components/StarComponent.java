package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.shape.Circle;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;

/**
 * Component for scrolling stars with vertical wrapping and optional twinkle effect.
 */
public class StarComponent extends Component {

    private final double scrollSpeed;
    private final double baseOpacity;
    private final Circle circle;
    
    private double twinklePhase;
    private final double twinkleSpeed;
    private final double twinkleAmount;

    public StarComponent(Circle circle, double scrollSpeed, double baseOpacity, double twinkleSpeed, double twinkleAmount) {
        this.circle = circle;
        this.scrollSpeed = scrollSpeed;
        this.baseOpacity = baseOpacity;
        this.twinkleSpeed = twinkleSpeed;
        this.twinkleAmount = twinkleAmount;
        this.twinklePhase = Math.random() * Math.PI * 2;
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateY(scrollSpeed * tpf);
        
        if (entity.getY() > getAppHeight() + 10) {
            entity.setY(-10);
        }
        
        if (twinkleAmount > 0) {
            twinklePhase += twinkleSpeed * tpf;
            double opacityMod = Math.sin(twinklePhase) * twinkleAmount;
            circle.setOpacity(Math.max(0.1, Math.min(1.0, baseOpacity + opacityMod)));
        }
    }
}
