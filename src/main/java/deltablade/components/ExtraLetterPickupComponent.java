package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

public class ExtraLetterPickupComponent extends Component {
    
    private final char letter;
    private final int letterIndex;
    private double speed = 65;
    private double pulsePhase = 0;
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
                if (child instanceof Group shineGroup) {
                    for (Node shineChild : shineGroup.getChildren()) {
                        if (shineChild instanceof Rectangle rect && rect.getWidth() == 4) {
                            shineStripe = rect;
                            break;
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);
        
        pulsePhase += tpf * 4;
        double scale = 1 + Math.sin(pulsePhase) * 0.12;
        entity.setScaleX(scale);
        entity.setScaleY(scale);
        
        if (shineStripe != null) {
            shinePhase += tpf * 50;
            double shineX = -18 + (shinePhase % 44);
            if (shineX > 26) {
                shinePhase = 0;
                shineX = -18;
            }
            shineStripe.setTranslateX(shineX);
        }
        
        double playableLeft = GameVars.RAIL_WIDTH;
        double playableRight = FXGL.getAppWidth() - GameVars.RAIL_WIDTH;
        double centerX = entity.getX() + 16;
        if (centerX < playableLeft) {
            entity.setX(playableLeft - 16);
        } else if (centerX > playableRight) {
            entity.setX(playableRight - 16);
        }
        
        if (entity.getY() > FXGL.getAppHeight() + 20) {
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
