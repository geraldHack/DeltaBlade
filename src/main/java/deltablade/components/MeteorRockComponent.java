package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

public class MeteorRockComponent extends Component {

    private final double baseSpeed;
    private final double driftX;
    private final double spin;
    private double speedScale = 1.0;

    public MeteorRockComponent(double baseSpeed, double driftX, double spin) {
        this.baseSpeed = baseSpeed;
        this.driftX = driftX;
        this.spin = spin;
    }

    public void setSpeedScale(double speedScale) {
        this.speedScale = speedScale;
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateX(driftX * speedScale * tpf);
        entity.translateY(baseSpeed * speedScale * tpf);
        entity.rotateBy(spin * tpf);

        if (entity.getY() > FXGL.getAppHeight() + 50) {
            entity.removeFromWorld();
        }
    }
}
