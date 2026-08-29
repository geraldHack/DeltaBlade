package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;
import javafx.scene.Group;
import javafx.scene.text.Text;

public class ExtraLetterPickupComponent extends Component {
    
    private final char letter;
    private final int letterIndex;
    private final Text letterText;
    private final Group flipWrapper;
    private double speed = 80;
    private double flipPhase = 0;
    
    public ExtraLetterPickupComponent(char letter, int letterIndex, Text letterText) {
        this.letter = letter;
        this.letterIndex = letterIndex;
        this.letterText = letterText;
        this.flipWrapper = letterText != null ? new Group(letterText) : null;
    }
    
    public Group getFlipWrapper() {
        return flipWrapper;
    }
    
    @Override
    public void onAdded() {
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);
        
        if (flipWrapper != null) {
            flipPhase += tpf * 2.5;
            double scaleX = Math.cos(flipPhase);
            if (Math.abs(scaleX) < 0.18) {
                scaleX = scaleX < 0 ? -0.18 : 0.18;
            }
            flipWrapper.setScaleX(scaleX);
        }
        
        double playableLeft = GameVars.RAIL_WIDTH;
        double playableRight = FXGL.getAppWidth() - GameVars.RAIL_WIDTH;
        double centerX = entity.getX() + 16;
        if (centerX < playableLeft) {
            entity.setX(playableLeft - 16);
        } else if (centerX > playableRight) {
            entity.setX(playableRight - 16);
        }
        
        if (entity.getY() > FXGL.getAppHeight() + 80) {
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
