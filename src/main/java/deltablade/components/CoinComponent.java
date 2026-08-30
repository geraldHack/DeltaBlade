package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

public class CoinComponent extends Component {
    
    public enum CoinType {
        WHITE(10, 80),
        GREEN(50, 80),
        BLUE(100, 80),
        VIOLET(1000, 240);
        
        public final int value;
        public final double fallSpeed;
        
        CoinType(int value, double fallSpeed) {
            this.value = value;
            this.fallSpeed = fallSpeed;
        }
    }
    
    private final CoinType coinType;
    
    public CoinComponent(CoinType coinType) {
        this.coinType = coinType;
    }
    
    public int getValue() {
        return coinType.value;
    }
    
    public CoinType getCoinType() {
        return coinType;
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translateY(coinType.fallSpeed * tpf);
        
        if (entity.getY() > FXGL.getAppHeight()) {
            entity.removeFromWorld();
        }
    }
}
