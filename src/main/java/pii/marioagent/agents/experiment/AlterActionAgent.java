package pii.marioagent.agents.experiment;

import pii.marioagent.Main;
import pii.marioagent.agents.meta.Recorder;
import pii.marioagent.environnement.Action;
import pii.marioagent.environnement.Description;
import pii.marioagent.environnement.ForwardModel;
import pii.marioagent.environnement.RandomAction;

public class AlterActionAgent extends BaseExperimentAgent {

    Description from;

    public AlterActionAgent(RandomAction randomAction, Recorder actionRecorder, Description inspiration) {
        super(randomAction, actionRecorder);
        this.from = inspiration;
    }

    /**
     * TODO: doc
     * 
     * @param d
     * @return
     */
    public Action alterAction(Description d) {
        Action action = d.getAction();
        action.reset();

        int range = Math.min((int) (action.length / 2f), 10);

        int before = this.random.nextInt(range);
        int after = this.random.nextInt(range);

        int start = 0 + this.random.nextInt(range);
        int end = action.length - 1 - this.random.nextInt(range);

        boolean[][] inputs = new boolean[before + (end - start + 1) + after][];

        // copy from `start` to `end` (offset by `before`)
        boolean isPrevRandom = false;
        for (int k = 0; k < end + 1; k++) {
            boolean[] frame = action.consume();
            if (start - 1 < k) {
                double r = this.random.nextDouble();
                boolean isNewRandom = isPrevRandom && r < Main.SETTINGS.alter.actionMutationPropagate
                        || r < Main.SETTINGS.alter.actionMutationRate / action.length;

                if (isNewRandom)
                    frame = this.random.nextInputs(frame);
                isPrevRandom = isNewRandom;

                inputs[before - start + k] = frame;
            }
        }

        // end with `after` new random inputs
        for (int k = 0; k < after; k++)
            inputs[before - start + end + k + 1] = this.random.nextInputs(inputs[before - start + end + k]);

        // start with `before` new random inputs
        for (int k = before; 0 < k; k--)
            inputs[k - 1] = this.random.nextInputs(inputs[k]);

        return new Action(inputs);
    }

    @Override
    protected String getExpName() {
        return "alter action";
    }

    @Override
    protected Description[] getExpInspiration(ForwardModel model) {
        return new Description[] { this.from };
    }

    @Override
    protected Description getExpResult(ForwardModel model, Description... from) {
        Action r = this.alterAction(from[0]);
        return super.newDescription(from[0].getGrid(), from[0].getPreferredLocation(), r);
    }

}
