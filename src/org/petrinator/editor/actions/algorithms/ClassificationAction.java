/*
 * Copyright (C) 2008-2010 Martin Riesz <riesz.martin at gmail.com>
 * Copyright (C) 2016-2017 Joaquin Rodriguez Felici <joaquinfelici at gmail.com>
 * Copyright (C) 2016-2017 Leandro Asson <leoasson at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.petrinator.editor.actions.algorithms;

import org.petrinator.editor.Root;
import org.petrinator.editor.actions.algorithms.newReachability.CRTree;
import org.petrinator.editor.filechooser.*;
import org.petrinator.petrinet.*;
import org.petrinator.util.GraphicsTools;
import pipe.calculations.myTree;
import pipe.exceptions.EmptyNetException;
import pipe.gui.ApplicationSettings;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.PetriNetChooserPanel;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.utilities.writers.PNMLWriter;
import pipe.views.MarkingView;
import pipe.views.PetriNetView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author Joaquin Felici <joaquinfelici at gmail.com>
 */
public class ClassificationAction extends AbstractAction
{
    private static final String MODULE_NAME = "Net classification";
    private ResultsHTMLPane results;
    private Root root;
    private JDialog guiDialog;
    private ButtonBar classifyButton;

    public ClassificationAction(Root root)
    {
        String name = "Net classification";
        this.root = root;
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/classification16.png"));

        guiDialog =  new JDialog(root.getParentFrame(), MODULE_NAME, true);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        results = new ResultsHTMLPane("");
        contentPane.add(results);

        classifyButton = new ButtonBar("Classify", new ClassifyListener(), guiDialog.getRootPane());
        contentPane.add(classifyButton);
    }

    public void actionPerformed(ActionEvent e)
    {

        results.setText("");

        // Disables the copy and save buttons
        results.setEnabled(false);

        // Enables classify button
        classifyButton.setButtonsEnabled(true);

        // Shows initial pane
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
    }

    /**
     * Classify button click handler
     */
    private class ClassifyListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {

            // Checks if the net is valid
            if(!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
                JOptionPane.showMessageDialog(null, "Invalid Net!", "Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }

            classifyButton.setButtonsEnabled(false);

            String s = "<h2>Petri Net Classification</h2>";

            try {


                /*
                 * Information for boundedness, safeness and deadlock
                 */
                CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);

                s += "<h3>Mathematical Properties</h3>";

                String[] treeInfo = new String[]{
                        "", "",
                        "Bounded", "" + statesTree.isBounded(),
                        "Safe", "" + statesTree.isSafe(),
                        "Deadlock", "" + statesTree.hasDeadlock()
                };

                s += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, true, true);

                if(statesTree.hasDeadlock())
                {
                    s += "<h3 style=\"margin-top:20px\">Shortest Path to Deadlock</h3>";
                    s += "<div style=\"margin-top:10px\">"+statesTree.getShortestPathToDeadlock()+"</div>";
                }


                /*
                 * Standard classification
                 */
                /*s += ResultsHTMLPane.makeTable(new String[]{"&nbsp&emsp Types of Petri net &emsp&nbsp", "&emsp&emsp&emsp",
                        "State Machine", "" + stateMachine(sourceDataLayer),
                        "Marked Graph", "" + markedGraph(sourceDataLayer),
                        "Free Choice Net", "" + freeChoiceNet(sourceDataLayer),
                        "Extended FCN", "" + extendedFreeChoiceNet(sourceDataLayer),
                        "Simple Net", "" + simpleNet(sourceDataLayer),
                        "Extended SN", "" + extendedSimpleNet(sourceDataLayer)
                }, 2, false, true, false, true);*/

                /*
                 * Bounded/safe/deadlock
                 */

                s += "<h3>State Machine: </h3>"+stateMachine(root.getDocument().getPetriNet());
                s += "<h3>Marked Graph: </h3>"+markedGraph(root.getDocument().getPetriNet());
                s += "<h3>Free Choice Net: </h3>"+freeChoiceNet(root.getDocument().getPetriNet());


                results.setEnabled(true);

            }
            catch(OutOfMemoryError e)
            {
                System.gc();
                results.setText("");
                s = "Memory error: " + e.getMessage();

                s += "<br>Not enough memory. Please use a larger heap size." +
                        "<br>" + "<br>Note:" +
                        "<br>The Java heap size can be specified with the -Xmx option." +
                        "<br>E.g., to use 512MB as heap size, the command line looks like this:" +
                        "<br>java -Xmx512m -classpath ...\n";
                results.setText(s);
            }
            catch (StackOverflowError e){
                results.setText("An error has occurred, the net might have too many states...");
            }
            catch(Exception e)
            {
                e.printStackTrace();
                s = "<br>Error" + e.getMessage();
                results.setText(s);
            }

            results.setText(s);

        }


    };


    /**
     * State machine detection
     *
     * @return true if and only if all transitions have at most one input or output
     */
    private boolean stateMachine(PetriNet petriNet)
    {
        ArrayList<Node> sortedTransitions = petriNet.getSortedTransitions();

        for (Node transition : sortedTransitions) {

            if (transition.getConnectedArcsToNode().size() > 1 || transition.getConnectedArcsFromNode().size() > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Marked graph detection
     *
     * @return true if and only if all places have at most one input or output
     */
    boolean markedGraph(PetriNet petriNet)
    {

        ArrayList<Node> sortedPlaces = petriNet.getSortedPlaces();

        for (Node place : sortedPlaces) {

            if(place.getConnectedArcsToNode().size() > 1 || place.getConnectedArcsFromNode().size() > 1){
                return false;
            }
        }

        return true;
    }

    /**
     * Free choice net detection
     *
     * @return true iff no places' outputs go to the same transition, unless those places both have only one output
     */
    boolean freeChoiceNet(PetriNet petriNet)
    {
        ArrayList<Node> sortedTransitions = petriNet.getSortedTransitions();

        for (Node transition: sortedTransitions) {

            Set<Node> inputPlaces = transition.getInputNodes();

            if(inputPlaces.size() > 1){

                for(Node place: inputPlaces){
                    if(place.getConnectedArcsFromNode().size() > 1){
                        return false;
                    }
                }
            }


        }

        return true;
    }

    /**
     * Extended free choice net detection
     *
     * @param pnmlData
     * @return true iff no places' outputs go to the same transition, unless both places outputs are identical
     *         EFC-net iff &forall; p, p&prime; &isin; P: p &#x2260; p&prime; &#x21d2; (p&bull;&#x2229;p&prime;&bull; = 0 or p&bull; = p&prime;&bull;)
     *         <pre>
     *         P - T
     *         P - T       Yes (no common outputs)
     *           \
     *             T
     *
     *         P - T
     *           /
     *         P - T       No (common outputs, outputs not identical)
     *
     *         P - T
     *           X
     *         P - T       Yes (common outputs identical) *** only addition to normal free choice net
     *
     *         P - T       Yes (common outputs identical)
     *           /
     *         P
     *         </pre>
     * @author Maxim Gready after James D Bloom
     */
    protected boolean extendedFreeChoiceNet(PetriNet petriNet)
    {


        /*int[] fps1, fps2; // forwards place sets for p and p'



        for(int placeNo = 0; placeNo < placeCount; placeNo++)
        {
            for(int placeDashNo = placeNo + 1; placeDashNo < placeCount; placeDashNo++)
            {
                fps1 = forwardsPlaceSet(pnmlData, placeNo);
                fps2 = forwardsPlaceSet(pnmlData, placeDashNo);
                if(intersectionBetweenSets(fps1, fps2) && !Arrays.equals(fps1, fps2))
                {
                    return false;
                }
            }
        }*/
        return true;
    }

    /**
     * Simple net (SPL-net) detection
     *
     * @param pnmlData
     * @return true iff no places' outputs go to the same transition, unless one of the places only has one output
     *         SPL-net iff &forall; p, p&prime; &isin; P: p &#x2260; p&prime; &#x21d2; (p&bull;&#x2229;p&prime;&bull; = 0 or |p&bull;| &#8804; 1 or |p&prime;&bull;| &#8804; 1)
     *         <pre>
     *         P - T
     *         P - T       true (no common outputs)
     *           \
     *             T
     *
     *         P - T
     *           /         true (common outputs, both only have one)
     *         P
     *
     *         P - T
     *           /         true (common outputs, one place has only one)
     *         P - T
     *
     *         P - T
     *           X         false (common outputs, neither has only one)
     *         P - T
     *
     *         P - T
     *           \
     *             T       false (common outputs, neither has only one)
     *           /
     *         P - T
     *         </pre>
     * @author Maxim Gready after James D Bloom
     */
    boolean simpleNet(PetriNetView pnmlData)
    {
        int placeCount = pnmlData.numberOfPlaces();

        for(int placeNo = 0; placeNo < placeCount; placeNo++)
        {
            for(int placeDashNo = 0; placeDashNo < placeCount; placeDashNo++)
            {
                if(placeDashNo != placeNo)
                {
                    int[] placeSet = forwardsPlaceSet(pnmlData, placeNo);
                    int[] placeDashSet = forwardsPlaceSet(pnmlData, placeDashNo);
                    if(intersectionBetweenSets(placeSet, placeDashSet) &&
                            (countPlaceOutputs(pnmlData, placeNo) > 1) &&
                            (countPlaceOutputs(pnmlData, placeDashNo) > 1))
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Extended simple net (ESPL-net) detection
     *
     * @param pnmlData
     * @return true iff no places' outputs go to the same transition, unless one of the places' outputs is a subset of or equal to the other's
     *         ESPL-net iff &forall; p, p&prime; &isin; P: p &#x2260; p&prime; &#x21d2; (p&bull;&#x2229;p&prime;&bull; = 0 or p&bull; &#x2286; p&prime;&bull; or p&prime;&bull; &#x2286; p&bull;)
     *         <pre>
     *         P - T
     *         P - T       Yes (no common outputs)
     *           \
     *             T
     *
     *         P - T
     *           /
     *         P - T       Yes (common outputs, first place's outputs is subset of second)
     *
     *         P - T
     *           X
     *         P - T       Yes (common outputs, identical)
     *
     *         P - T       Yes (common outputs, identical)
     *           /
     *         P
     *
     *         P - T
     *           \
     *             T       false (common outputs, neither is subset of other)
     *           /
     *         P - T
     *
     *             T
     *           /
     *         P - T       true (common outputs, second's is subset of first's)
     *           X
     *         P - T
     *         </pre>
     * @author Maxim Gready after James D Bloom
     */
    boolean extendedSimpleNet(PetriNetView pnmlData)
    {
        int placeCount = pnmlData.numberOfPlaces();

        for(int placeNo = 0; placeNo < placeCount; placeNo++)
        {
            for(int placeDashNo = 0; placeDashNo < placeCount; placeDashNo++)
            {
                if(placeDashNo != placeNo)
                {
                    int[] placeSet = forwardsPlaceSet(pnmlData, placeNo);
                    int[] placeDashSet = forwardsPlaceSet(pnmlData, placeDashNo);
                    if(intersectionBetweenSets(placeSet, placeDashSet) &&
                            (!isSubset(placeSet, placeDashSet)))
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Counts inputs to given place
     *
     * @param pnmlData
     * @param PlaceNo
     * @return number of arcs leading to the given place number; -1 on error
     */
    private int countPlaceInputs(PetriNetView pnmlData, int PlaceNo)
    {

        int[][] backwards = pnmlData.getActiveTokenView().getForwardsIncidenceMatrix(
                pnmlData.getArcsArrayList(),
                pnmlData.getTransitionsArrayList(), pnmlData.getPlacesArrayList());
        // The forwards incidence matrix is like this:
        // | 0 1 |     P0 ->- T0 ->- P1
        // | 1 0 |       `-<- T1 -<-'
        // where rows are place numbers and columns are transition numbers.
        // The number is the weight of the arc from the corresponding transition
        // to the corresponding place.
        // It can also be considered the backwards incidence matrix for the place.
        int count = 0;

        if(PlaceNo < backwards.length)
        {
            for(int TransitionNo = 0; TransitionNo < backwards[PlaceNo].length; TransitionNo++)
            {
                if(backwards[PlaceNo][TransitionNo] != 0)
                {
                    count++;
                }
            }
        }
        else
        {
            return -1;
        }
        return count;
    }

    // Counts outputs from given place
    // @return number of arcs leading from the given place number; -1 on error
    private int countPlaceOutputs(PetriNetView pnmlData, int PlaceNo)
    {
        int[][] forwards = pnmlData.getActiveTokenView().getBackwardsIncidenceMatrix(
                pnmlData.getArcsArrayList(),
                pnmlData.getTransitionsArrayList(), pnmlData.getPlacesArrayList()); // transition backwards = place forwards
        int count = 0;

        if(PlaceNo >= forwards.length)
        {
            return -1;
        }

        for(int TransitionNo = 0; TransitionNo < forwards[PlaceNo].length; TransitionNo++)
        {
            if(forwards[PlaceNo][TransitionNo] != 0)
            {
                count++;
            }
        }
        return count;
    }

    // Counts inputs to given transition
    // @return number of arcs leading to the given transition number; -1 on error
    private int countTransitionInputs(PetriNetView pnmlData, int TransitionNo)
    {
        int[][] backwards = pnmlData.getActiveTokenView().getBackwardsIncidenceMatrix(pnmlData.getArcsArrayList(),
                pnmlData.getTransitionsArrayList(), pnmlData.getPlacesArrayList());
        int count = 0;

        if((backwards.length < 1) || (TransitionNo >= backwards[0].length))
        {
            return -1;
        }
        for(int[] backward : backwards)
        {
            if(backward[TransitionNo] != 0)
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts outputs from given transition
     *
     * @param pnmlData
     * @param TransitionNo
     * @return number of arcs leading from the given transition number; -1 on error
     */
    private int countTransitionOutputs(PetriNetView pnmlData, int TransitionNo)
    {
        int[][] forwards = pnmlData.getActiveTokenView().getForwardsIncidenceMatrix(pnmlData.getArcsArrayList(),
                pnmlData.getTransitionsArrayList(), pnmlData.getPlacesArrayList());
        int count = 0;

        if((forwards.length < 1) || (TransitionNo >= forwards[0].length))
        {
            return -1;
        }

        for(int[] forward : forwards)
        {
            if(forward[TransitionNo] != 0)
            {
                count++;
            }
        }
        return count;
    }

    // returns array of ints set to indices of transitions output to by given place
    private int[] forwardsPlaceSet(PetriNetView pnmlData, int PlaceNo)
    {
        int[][] forwards = pnmlData.getActiveTokenView().getBackwardsIncidenceMatrix(pnmlData.getArcsArrayList(),
                pnmlData.getTransitionsArrayList(), pnmlData.getPlacesArrayList());
        ArrayList postsetArrayList = new ArrayList();

        for(int TransitionNo = 0; TransitionNo < forwards[PlaceNo].length; TransitionNo++)
        {
            if(forwards[PlaceNo][TransitionNo] != 0)
            {
                postsetArrayList.add(new Integer(TransitionNo));
            }
        }

        int[] postset = new int[postsetArrayList.size()];

        for(int postsetPosition = 0; postsetPosition < postset.length; postsetPosition++)
        {
            postset[postsetPosition] =
                    ((Integer) postsetArrayList.get(postsetPosition)).intValue();
        }

        return postset;
    }

    boolean intersectionBetweenSets(int[] setOne, int[] setTwo)
    {
        for(int aSetOne : setOne)
        {
            for(int twoPosition = 0; twoPosition < setTwo.length; twoPosition++)
            {
                if(aSetOne == setTwo[twoPosition])
                {
                    return true;
                }
            }
        }
        return false;
    }

    // returns true if either array is a subset of the other
    private static boolean isSubset(int[] A, int[] B)
    {
        boolean isSubsetOf = false;

        // first you must eliminate all dublicate values from the sets, so run
        // through each set and replace all dublicates with an inpossible (flag)
        // value, e.g. -1
        int[] AA = reduce(A);
        int[] BB = reduce(B);

        // then look for the set with smaller cardinality being subset of the set
        // with greater cardinality
        // make sure 1st agument has smaller cardinality than 2nd
        if(cardinality(AA) < cardinality(BB))
        {
            isSubsetOf = findSubset(AA, BB);
        }
        else
        {
            isSubsetOf = findSubset(BB, AA);
        }
        return isSubsetOf;
    }

    // eliminate all duplicate values from the set, so run through the set
    // and replace all duplicates with an inpossible (flag) value, e.g. -1
    private static int[] reduce(int[] X)
    {

        for(int i = 0; i < X.length; i++)
        {
            for(int j = 0; j < i; j++)
            { // check values before current index
                if(X[i] == X[j])
                {// value already exists in the set
                    X[i] = -1; // flag dublicate
                    break;  // and break the inner loop
                }
            }
        }
        return X;
    }

    // determine set cardinality (number of distinct values)
    private static int cardinality(int[] X)
    {
        int c = 0; // cardinality

        for(int aX : X)
        {
            if(aX != -1)
            {
                c++;
            }
        }
        return c;
    }

    // A has smaller cardinality than B, so only A can be subset of B and not the opposite
    private static boolean findSubset(int[] A, int[] B)
    {
        boolean elementExists;

        // loop through A and see if each of its elements is contained in B
        // if at least one is not contained, then it is not a subset. watch for flags -1
        for(int aA : A)
        {
            elementExists = false;
            for(int j = 0; j < B.length; j++)
            {
                if(aA == B[j] && aA != -1)
                { // ith element of A exists in B
                    elementExists = true;
                    break;
                }
            }
            // test for particular ith element of A in all of B
            if(!elementExists)
            {
                return false;
            }
        }
        return true;
    }

}
