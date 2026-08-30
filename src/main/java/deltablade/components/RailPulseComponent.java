package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;

/**
 * Slow opacity pulse for the inner energy strip on a side rail.
 */
public class RailPulseComponent extends Component {

    private final Node energy;
    private double time;

    public RailPulseComponent(Node energy) {
        this.energy = energy;
    }

    @Override
    public void onUpdate(double tpf) {
        time += tpf;
        energy.setOpacity(0.55 + 0.35 * Math.sin(time * 2.4));
    }
}
