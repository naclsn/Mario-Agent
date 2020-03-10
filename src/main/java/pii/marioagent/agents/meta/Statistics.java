package pii.marioagent.agents.meta;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.marioai.engine.core.MarioRender;
import org.marioai.engine.core.MarioRender.AddedRender;

import pii.marioagent.environnement.Description;
import pii.marioagent.environnement.ForwardModel;
import pii.marioagent.environnement.utils.TilePos;

public class Statistics implements AddedRender {

    private float percent;

    private Description choice;
    private TilePos choiceAt;

    private Description previous;

    private HashMap<Description, ArrayList<Float>> records;

    public Statistics() {
        this.records = new HashMap<>();
    }

    /**
     * Used to follow the progress of the agent.
     * 
     * @param model environnement.
     */
    public void progressReport(ForwardModel model) {
        if (this.previous != null) {
            float newPercent = model.getCompletionPercentage();
            float diff = newPercent - this.percent;

            // System.out.print((diff < 0 ? "" : "+") + 100 * diff + "% by doing: '");
            // System.out.print(this.previous.getAction() + "'");

            ArrayList<Float> gains = this.records.get(this.previous);
            if (gains == null) {
                gains = new ArrayList<>();
                this.records.put(this.previous, gains);
                // System.out.println();
            } // else System.out.println(" (for the " + (gains.size() + 1) + "th time)");
            gains.add(diff);

            this.percent = newPercent;
            this.previous = null;
        }
    }

    /**
     * Records the elements the agent used to describe its environnement and which
     * it chose as most important.
     * 
     * @param choices the description the agent found fitting.
     * @param which   the description the agent found most fitting.
     */
    public void choiceReport(Description[] found, TilePos[] at, int choice) {
        this.previous = this.choice;
        this.choice = found[choice];
        this.choiceAt = at[choice];
    }

    /**
     * Records any other noticeable events.
     * 
     * @param status the status to report.
     */
    public void statusReport(Statistics.Status status) {
        // System.out.println(status + " reported");
        if (status == Status.DEAD_END)
            this.choice = null;
    }

    public enum Status {
        DEAD_END
    }

    /**
     * Returns the best entry in the records as a 'pair' of a Description and the
     * gains in percent.
     * 
     * @return the best entry in records.
     */
    public Map.Entry<Description, ArrayList<Float>> getBest() {
        Map.Entry<Description, ArrayList<Float>> r = null;

        float best = -1f;
        for (Map.Entry<Description, ArrayList<Float>> pair : this.records.entrySet()) {
            float max = Collections.max(pair.getValue());
            if (best < max) {
                r = pair;
                best = max;
            }
        }

        return r;
    }

    /**
     * Returns the best entries in the records as a list of 'pairs' of a Description
     * and the gains in percent.
     * 
     * @return the best entry in records.
     */
    public List<Entry<Description, ArrayList<Float>>> getBests() {
        ArrayList<Entry<Description, ArrayList<Float>>> r = new ArrayList<>(this.records.entrySet());
        r.sort((Entry<Description, ArrayList<Float>> e1, Entry<Description, ArrayList<Float>> e2) -> Collections
                .max(e1.getValue()) < Collections.max(e2.getValue()) ? 1 : -1);
        return r;
    }

    /**
     * Returns the n best entries in the records as a list of 'pairs' of a
     * Description and the gains in percent.
     * 
     * @param n number of entries to return
     * @return the best entry in records.
     */
    public List<Entry<Description, ArrayList<Float>>> getBests(int n) {
        return this.getBests().subList(0, n);
    }

    @Override
    public void render(Graphics g, MarioRender r) {
        Description d = this.choice;

        if (d == null) return;

        r.drawStringDropShadow(g, "Description tag: " + d.tag, 0, 1, 7);
        for (int i = 0; i < d.width; i++)
            for (int j = 0; j < d.height; j++)
                r.drawStringDropShadow(g, d.getAt(i, j) < 0 ? "?" : d.getAt(i, j) == 0 ? "-" : "X", 2 * (i + this.choiceAt.x), 2 * (j + this.choiceAt.y), 7);
    }

}
