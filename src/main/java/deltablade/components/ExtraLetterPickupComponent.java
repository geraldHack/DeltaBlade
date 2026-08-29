package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

public class ExtraLetterPickupComponent extends Component {
    
    private final char letter;
    private final int letterIndex;
    private double speed = 70;
    private double pulsePhase = 0;
    private double wobblePhase = 0;
    
    public ExtraLetterPickupComponent(char letter, int letterIndex) {
        this.letter = letter;
        this.letterIndex = letterIndex;
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);
        
        pulsePhase += tpf * 6;
        double scale = 1 + Math.sin(pulsePhase) * 0.2;
        entity.setScaleX(scale);
        entity.setScaleY(scale);
        
        wobblePhase += tpf * 4;
        entity.setRotation(Math.sin(wobblePhase) * 8);
        
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
