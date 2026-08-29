package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.text.Text;

public class ExtraLetterPickupComponent extends Component {
    
    private final char letter;
    private final int letterIndex;
    private double speed = 65;
    private double flipPhase = 0;
    private Text letterText;
    
    private static final double MIN_SCALE_X = 0.18;
    
    public ExtraLetterPickupComponent(char letter, int letterIndex) {
        this.letter = letter;
        this.letterIndex = letterIndex;
    }
    
    @Override
    public void onAdded() {
        Node view = entity.getViewComponent().getChildren().get(0);
        letterText = findTextInGroup(view);
    }
    
    private Text findTextInGroup(Node node) {
        if (node instanceof Text t) {
            return t;
        }
        if (node instanceof Group group) {
            for (Node child : group.getChildren()) {
                Text found = findTextInGroup(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);
        
        flipPhase += tpf * 4.0;
        double rawScale = Math.cos(flipPhase);
        double clampedScale = Math.signum(rawScale) * Math.max(Math.abs(rawScale), MIN_SCALE_X);
        
        if (letterText != null) {
            letterText.setScaleX(clampedScale);
        }
        
        double playableLeft = GameVars.RAIL_WIDTH;
        double playableRight = FXGL.getAppWidth() - GameVars.RAIL_WIDTH;
        double centerX = entity.getX() + 16;
        if (centerX < playableLeft) {
            entity.setX(playableLeft - 16);
        } else if (centerX > playableRight) {
            entity.setX(playableRight - 16);
        }
        
        if (entity.getY() > FXGL.getAppHeight() + 40) {
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
