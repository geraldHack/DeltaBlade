package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

public class ExtraLetterPickupComponent extends Component {
    
    private final char letter;
    private final int letterIndex;
    private double speed = 55;
    private double pulsePhase = 0;
    private double rotatePhase = 0;
    private double shinePhase = 0;
    private Rectangle shineStripe;
    
    public ExtraLetterPickupComponent(char letter, int letterIndex) {
        this.letter = letter;
        this.letterIndex = letterIndex;
    }
    
    @Override
    public void onAdded() {
        Node view = entity.getViewComponent().getChildren().get(0);
        if (view instanceof Group group) {
            for (Node child : group.getChildren()) {
                if (child instanceof Rectangle rect && rect.getWidth() == 4) {
                    shineStripe = rect;
                    break;
                }
            }
        }
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);
        
        pulsePhase += tpf * 5;
        double scale = 1 + Math.sin(pulsePhase) * 0.15;
        entity.setScaleX(scale);
        entity.setScaleY(scale);
        
        rotatePhase += tpf * 2.5;
        double rotateY = Math.sin(rotatePhase) * 20;
        entity.getViewComponent().getChildren().get(0).setScaleX(Math.cos(Math.toRadians(rotateY)));
        
        if (shineStripe != null) {
            shinePhase += tpf * 60;
            double shineX = -18 + (shinePhase % 40);
            if (shineX > 22) {
                shinePhase = 0;
                shineX = -18;
            }
            shineStripe.setTranslateX(shineX);
        }
        
        if (entity.getY() > FXGL.getAppHeight()) {
            entity.removeFromWorld();
        }
    }
    
    public char getLetter() {
        return letter;
    }
    
    public int getLetterIndex() {
        return letterIndex;
    }
}
