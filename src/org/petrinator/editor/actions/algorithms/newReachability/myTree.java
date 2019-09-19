package org.petrinator.editor.actions.algorithms.newReachability;

import org.petrinator.editor.Root;
import org.petrinator.petrinet.Marking;
import pipe.exceptions.ImmediateAbortException;
import pipe.exceptions.TreeTooBigException;
import pipe.io.ReachabilityGraphFileHeader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class myTree {

    private boolean foundAnOmega = false;            //bounded
    private boolean moreThanOneToken = false;        //safe
    private boolean noEnabledTransitions = false;    //deadlock

    private ArrayList<int[]> statesList;

    private TreeNode root;                             //root of the tree
    private int nodeCount = 0;                       //total number of nodes

    //Petri net matrices: TODO : final?
    private int [][] _CPlus;
    private int [][] _CMinus;
    private int [][] _C;
    private int [][] _inhibition;
    private int [][] _reset;
    private int [][] _reader;

    private boolean hasInhibitionArcs;
    private boolean hasResetArcs;
    private boolean hasReaderArcs;

    private final int transitionCount;
    private final int placeCount;
    //public final int[] capacity;
    //public final int[] priority;
    private int[] pathToDeadlock;
    private final boolean tooBig = false;
    private int edges = 0;
    private int states = 0;

    private final Root petri_root;

    public myTree(Root petri_root, int[] initialMarking) throws TreeTooBigException{

        this.petri_root = petri_root;

        _CPlus = petri_root.getDocument().getPetriNet().forwardIMatrix();
        _CMinus = petri_root.getDocument().getPetriNet().backwardsIMatrix();
        _C = petri_root.getDocument().getPetriNet().incidenceMatrix();
        _inhibition = petri_root.getDocument().getPetriNet().inhibitionMatrix();
        _reset = petri_root.getDocument().getPetriNet().resetMatrix();
        _reader = petri_root.getDocument().getPetriNet().readerMatrix();


        hasInhibitionArcs = isMatrixNonZero(_inhibition);
        hasReaderArcs = isMatrixNonZero(_reader);
        hasResetArcs = isMatrixNonZero(_reset);

        //TODO add capacity/priority/timed if needed

        transitionCount = _CMinus[0].length;
        placeCount = _CMinus.length;//TODO view if values are right

        statesList = new ArrayList<>();

        root = new TreeNode(this, initialMarking, root, 1);



        //this.moreThanOneToken = isSafe(treeRoot);

        root.recursiveExpansion();

        System.out.printf("STATES - %d\n", statesList.size());
        for(int i=0; i<statesList.size(); i++){
            for(int j=0; j<placeCount; j++){
                System.out.printf("%2d ", statesList.get(i)[j]);
            }
            System.out.println("");
        }
    }

    boolean repeatedState(int[] marking){

        for(int i=0; i<statesList.size(); i++){
            if(Arrays.equals(statesList.get(i), marking)){
                return true;
            }
        }

        statesList.add(marking);
        return false;
    }

    int[] fire(int transition, int[] marking){

        int[] resultMarking = new int[placeCount];

        for(int i=0; i<placeCount; i++){
            resultMarking[i] = _C[i][transition] + marking[i];
        }

        if(hasResetArcs){
            for(int i=0; i<placeCount; i++){
                if(_reset[i][transition] != 0){
                    resultMarking[i] = 0;
                }
            }
        }

        return resultMarking;

    }

    /*public myTree(Root petri_root, int[] treeRoot, File reachabilityGraph)
            throws TreeTooBigException, ImmediateAbortException
    {
        this.petri_root = petri_root;

        _CPlus = petri_root.getDocument().getPetriNet().forwardIMatrix();
        _CMinus = petri_root.getDocument().getPetriNet().backwardsIMatrix();
        _C = petri_root.getDocument().getPetriNet().incidenceMatrix();
        _inhibition = petri_root.getDocument().getPetriNet().inhibitionMatrix();
        _reset = petri_root.getDocument().getPetriNet().resetMatrix();
        _reader = petri_root.getDocument().getPetriNet().readerMatrix();

        //TODO add capacity/priority/timed if needed

        transitionCount = _CMinus.length;
        placeCount = _CMinus[0].length;//TODO view if values are right

        root = new TreeNode(treeRoot, root, 1); //TODO view if tree reference needed

        //this.moreThanOneToken = isSafe(treeRoot);

        RandomAccessFile outputFile;
        RandomAccessFile esoFile;
        File intermediate = new File("graph.irg");

        if(intermediate.exists()){
            if(!intermediate.delete()){
                System.err.println("Could not delete intermediate file.");
            }
        }

        try
        {
            outputFile = new RandomAccessFile(intermediate, "rw");
            esoFile = new RandomAccessFile(reachabilityGraph, "rw");
            // Write a blank file header as a place holder for later
            ReachabilityGraphFileHeader header = new ReachabilityGraphFileHeader();
            header.write(esoFile);
            //Call expansion function on root of tree
            createCoverabilityGraph(outputFile, esoFile);
            outputFile.close();
        }
        catch(IOException e)
        {
            System.err.println("Could not create intermediate files.");
            return;
        }

        createCGFile(intermediate, esoFile, treeRoot.length, states, edges);

        if(intermediate.exists())
        {
            if(!intermediate.delete())
            {
                System.err.println("Could not delete intermediate file.");
            }
        }

    }

    //Determines if any place has more than one token for the current marking
    /*private boolean isSafe(final int[] treeRoot)
    {
        for(int aTreeRoot : treeRoot)
        {
            if(aTreeRoot > 1)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Function: void RecursiveExpansion()
     * Undertakes a recursive expansion of the tree
     * Called on root node from within the tree constructor.
     * @param outputFile
     * @param esoFile
     * @throws pipe.exceptions.TreeTooBigException
     * @throws pipe.io.ImmediateAbortException
     */
    /*private void createCoverabilityGraph(RandomAccessFile outputFile,
                                         RandomAccessFile esoFile) throws TreeTooBigException, pipe.io.ImmediateAbortException
    {
        int[] newMarkup; //mark used to create new node

        boolean repeatedNode; //attribute used for

        //int[] state = new int[placeCount];
        int[] state = root.getMarking();

        boolean[] enabledTransitions = areTransitionsEnabled(state);

        //writeNode(root.getID(),root.getMarking(),esoFile,true); TODO implementar ID
        states++;

        ArrayList<TreeNode> unprocessednodes = new ArrayList();

        unprocessednodes.add(root);
        TreeNode currentNode;
        while(!unprocessednodes.isEmpty()){

            currentNode = unprocessednodes.get(0);
            unprocessednodes.remove(0);

            state = currentNode.getMarking();

            enabledTransitions = areTransitionsEnabled(state);

            for (int i = 0; i< enabledTransitions.length; i++){
                if(enabledTransitions[i]){

                }
            }
        }

    }*/

    /**
     *
     * @param state current marking of the net
     * @return boolean array with true for enabled transitions
     */
    boolean [] areTransitionsEnabled(int [] state){

        boolean [] enabledTranitions = new boolean[transitionCount];

        for(int i = 0; i<transitionCount; i++){
            //for que recorre cada transicion
            enabledTranitions[i] = true;
            //comparo incidencia con marca
            for(int j=0; j<placeCount ; j++){
                if (_CMinus[j][i] > state[j]) {
                    enabledTranitions[i] = false;
                    break;
                }
            }

            if(hasInhibitionArcs){
                for(int j = 0; j < placeCount; j++){
                    boolean emptyPlace = state[j] == 0;
                    boolean placeInhibitsTransition = _inhibition[j][i] != 0;
                    if (!emptyPlace && placeInhibitsTransition) {
                        enabledTranitions[i] = false;
                        break;
                    }
                }
            }

            if(hasReaderArcs){
                for(int j=0; j<placeCount ; j++){
                    if (_reader[j][i] > state[j]) {
                        enabledTranitions[i] = false;
                        break;
                    }
                }
            }

        }

        return enabledTranitions;

    }

    private boolean isMatrixNonZero(int[][] matrix){
        // if the matrix is null or if all elements are zeros
        // the net does not have the type of arcs described by the matrix semantics
        try{
            for (int[] ints : matrix) {
                for (int j = 0; j < matrix[0].length; j++) {
                    if (ints[j] != 0)
                        return true;
                }
            }
            return false;
        } catch (NullPointerException e){
            return false;
        }
    }

}
