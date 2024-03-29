package pii.marioagent;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import pii.marioagent.agents.BaseAgent;
import pii.marioagent.agents.GenerateAgent;
import pii.marioagent.agents.UseAgent;
import pii.marioagent.agents.experiment.AlterActionAgent;
import pii.marioagent.agents.experiment.AlterDescriptionAgent;
import pii.marioagent.agents.meta.Recorder;
import pii.marioagent.agents.meta.Statistics;
import pii.marioagent.environnement.Description;
import pii.marioagent.environnement.RandomAction;
import pii.marioagent.environnement.repository.FileRepository;

public class Main {

    public static final boolean TRAIN = true;
    public static final boolean LOAD = false;
    public static final Settings SETTINGS = new Settings("./src/main/resources/levels/without-gaps.txt", "");
    public static final boolean QUIET = true;

    public static void main(String[] args) throws IOException {
        FileRepository repo = new FileRepository();
        if (Main.LOAD) repo.load(Paths.get(Main.SETTINGS.descRepoFilePath));

        if (Main.TRAIN)
        for (int k = 0; k < Main.SETTINGS.trainingIterationCount; k++) {
            System.out.println("Iteration " + k + ".");

            // create an agent to generate some descriptions
            RandomAction randomActionGenerate = new RandomAction(Main.SETTINGS.randomActionGenerateMinMax[0], Main.SETTINGS.randomActionGenerateMinMax[1]);
            BaseAgent generateAgent = new GenerateAgent(randomActionGenerate, new Recorder(repo));
            generateAgent.run(Main.SETTINGS.testLevelFilePath);

            if (!Main.QUIET) System.out.println(repo.count() + " entries to work with.");

            // create a new agent to use and evaluate the descriptions
            Statistics usage = new Statistics();
            new UseAgent(repo, usage).run(Main.SETTINGS.testLevelFilePath);
            List<Entry<Description, ArrayList<Float>>> bests = usage.getBests();

            // for each of the entries
            for (Entry<Description, ArrayList<Float>> pair : bests) {
                Description it = pair.getKey();
                float min = Collections.min(pair.getValue());
                float max = Collections.max(pair.getValue());

                it.setWeight(Main.SETTINGS.reWeighter.reWeight(it, min, max));

                if (!Main.QUIET) System.out.print(it.tag + ": from " + min + " to " + max);
                if (!Main.QUIET) System.out.println(" (" + it.getWeight() + " x " + it.getOccurences() + ")");

                // if it helped
                if (0 < min) {
                    // experiment on it
                    RandomAction randomActionExperiment = new RandomAction(Main.SETTINGS.randomActionExperimentMinMax[0], Main.SETTINGS.randomActionExperimentMinMax[1]);

                    BaseAgent alterAction = new AlterActionAgent(randomActionExperiment, new Recorder(repo), it);
                    alterAction.run(Main.SETTINGS.testLevelFilePath);

                    BaseAgent alterDescription = new AlterDescriptionAgent(randomActionExperiment, new Recorder(repo), it);
                    alterDescription.run(Main.SETTINGS.testLevelFilePath);
                } else {
                    // otherwise remove it
                    repo.remove(it);
                }
            }

            if (!Main.QUIET) System.out.println("\n");
        }

        System.out.println("Repository size: " + repo.count() + ".");
        if (Main.TRAIN) {
            System.out.println("Trimmed " + repo.trim(0f, true) + " unvalued elements.");
            repo.save(Paths.get(Main.SETTINGS.descRepoFilePath));
        }

        Graph graph = new SingleGraph("Descriptions Used");
        graph.display(true);

        Statistics visual = new Statistics(graph);
        new UseAgent(repo, visual).run(Main.SETTINGS.testLevelFilePath, Main.SETTINGS.trainedAgentSettings, visual);
    }

}
