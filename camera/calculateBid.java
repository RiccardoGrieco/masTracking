package camera;

import jason.*;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import jason.asSemantics.*;
import java.lang.Math;
import java.util.Iterator;
import java.lang.Exception;

/**
 * Implements the camera-agent's internal action 'calculateBid'.
 */
public class calculateBid extends DefaultInternalAction { 

    static final int K = 100;

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        Agent agent = ts.getAg();
        BeliefBase agentBB = agent.getBB();
        Literal tmpLiteral = agentBB.contains(Literal.parseLiteral("isFree"));
        boolean agentIsFree = tmpLiteral != null ? true : false;
        double  agentXCoord = 0.0,
                agentYCoord = 0.0,
                trackedXCoord = 0.0,
                trackedYCoord = 0.0,
                noNeighbors = 0.0,
                cost = 0.0,
                dist = 0.0,
                dist2 = 0.0;
        Unifier unifier;
        Iterator<Literal> itLiteral = null;

        //Retrieves agent's no. neightbors
        itLiteral = agentBB.getCandidateBeliefs(new PredicateIndicator("noNeighbors", 1));
        tmpLiteral = itLiteral.next();
        try {
            noNeighbors = Double.valueOf(tmpLiteral.getTermsArray()[0].toString());
        }
        catch(ArrayIndexOutOfBoundsException e) {
            throw new Exception("ERROR in internal action 'calculateBid': " 
                + "agent '" + agent + "' has literal 'noNeighbors' in BB with no arguments!");
        }

        //Retrieves agent's coordinates
        itLiteral = agentBB.getCandidateBeliefs(new PredicateIndicator("myPosition", 2));
        tmpLiteral = itLiteral.next();
        
        try {
            agentXCoord = Double.valueOf(tmpLiteral.getTermsArray()[0].toString());
            agentYCoord = Double.valueOf(tmpLiteral.getTermsArray()[1].toString());
        }
        catch(ArrayIndexOutOfBoundsException e) {
            throw new Exception("ERROR in internal action 'calculateBid': " 
                + "agent '" + agent + "' has literal 'position' in BB with less than 2 arguments!");
        }
        
        try {

            /*  
            *   predicate 'calculateBid' has 3 arguments:
            *   X: target's x coord (int)
            *   Y: target's y coord (int)
            *   B: values in witch will be the bid value (double)
            */
            if(args.length == 3 && args[0].isNumeric() && args[1].isNumeric()) {

                NumberTerm targetXCoord = (NumberTerm) args[0];
                NumberTerm targetYCoord = (NumberTerm) args[1];

                //Retrieves actual target's coords
                itLiteral = agentBB.getCandidateBeliefs(new PredicateIndicator("tracking", 4));
                
                
                //If agent is free
                if(itLiteral == null) {
                    dist = euclideanDistance(agentXCoord, agentYCoord, targetXCoord.solve(), targetYCoord.solve());
                    cost = ((1/dist) + K) / noNeighbors;
                }
                else {
                    tmpLiteral = itLiteral.next();

                    try {
                        trackedXCoord = Double.valueOf(tmpLiteral.getTermsArray()[2].toString());
                        trackedYCoord = Double.valueOf(tmpLiteral.getTermsArray()[3].toString());
                    }
                    catch(ArrayIndexOutOfBoundsException e) {
                        throw new Exception("ERROR in internal action 'calculateBid': " 
                            + "agent '" + agent + "' has literal 'tracking' in BB with less than 4 arguments!");
                    }

                    dist = euclideanDistance(agentXCoord, agentYCoord, trackedXCoord, trackedYCoord);
                    dist2 = euclideanDistance(agentXCoord, agentYCoord, targetXCoord.solve(), targetYCoord.solve());
                    cost = ((2 * dist) - dist2 - K) / noNeighbors;
                }
            }
            else {
                throw new Exception("The internal action 'calculateBid'" 
                    + "has received a wrong type arguments: firsts three arguments must be integers!");
            }

            // creates the term with the result
            NumberTerm result = new NumberTermImpl(cost);

            // unifies the result with the 3-rd argument of 'calculateBid'
            return un.unifies(result, args[2]);
            
        }
        catch(ArrayIndexOutOfBoundsException e) {
            throw new Exception("The internal action 'calculateBid'" 
                + "has not received three arguments!");
        }
    }

    /**
     * Computes the euclidean distance between points (x1, y1) and (x2, y2).
     */
    private double euclideanDistance(double x1, double y1, double x2, double y2) {

        double deltaX = x1 - x2;
        double deltaY = y1 - y2;
        return Math.sqrt(deltaX*deltaX + deltaY*deltaY);
    }
}