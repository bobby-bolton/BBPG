/*
 * Copyright (c) 2017, DThompson
 * All rights reserved.
 *
 * Licensed under: Creative Commons Attribution-NonCommercial 4.0 International Public License
 *  See LICENSE.TXT included with this code to read the full license agreement.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package PolyGlot.ManagersCollections;

import PolyGlot.DictCore;
import PolyGlot.Nodes.EtyExternalParent;
import PolyGlot.PGTUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This records parent->child relationships between lexical entries and serves
 * as the interface for interacting with etymological relationships between 
 * words.
 * 
 * @author Draque Thompson
 */
public class EtymologyManager {
    private final DictCore core;
    private final Map<Integer, List<Integer>> parentToChild = new HashMap<>();
    private final Map<Integer, List<Integer>> childToParent = new HashMap<>();
    private final Map<String, List<Integer>> extParentToChild = new HashMap<>();
    private final Map<Integer, Map<String, EtyExternalParent>> childToExtParent = new HashMap<>();
    private final List<String> allExtParents = new ArrayList<>();
    private Integer bufferParent = 0;
    private Integer bufferChild = 0;
    private EtyExternalParent bufferExtParent = new EtyExternalParent();
    
    public EtymologyManager(DictCore _core) {
        core = _core;
    }
    
    /**
     * Adds a parent->child relationship to two words if the relationship does
     * not already exist.
     * @param parent the parent
     * @param child the child
     * @throws IllegalLoopException if relationship creates looping dependancy
     */
    public void addRelation(Integer parent, Integer child) throws IllegalLoopException {
        ConWordCollection collection = core.getWordCollection();
        
        if (createsLoop(parent, child)) {
            throw new IllegalLoopException("Parent/Child relation creates illegal loop."
                    + " A word may never have itself in its own etymological lineage.");
        }
        
        // fail silently if either doesn't exist        
        if (!collection.exists(parent) || !collection.exists(child)) {
            return;
        }
        
        if (!parentToChild.containsKey(parent)) {
            List newList = new ArrayList<>();
            newList.add(child);
            parentToChild.put(parent, newList);
        } else {
            List myList = parentToChild.get(parent);
            
            if (!myList.contains(child)) {
                myList.add(child);
            }
        }
        
        if (!childToParent.containsKey(child)) {
            List newList = new ArrayList<>();
            newList.add(parent);
            childToParent.put(child, newList);
        } else {
            List myList = childToParent.get(child);
            
            if (!myList.contains(parent)) {
                myList.add(parent);
            }
        }
    }
    
    /**
     * Returns a list of children that a word has
     * @param wordId ID of word to retrieve children of
     * @return list of integer IDs of child words (empty list if none)
     */
    public List<Integer> getChildren(Integer wordId) {
        List<Integer> ret;
        
        if (parentToChild.containsKey(wordId)) {
            ret = parentToChild.get(wordId);
        } else {
            ret = new ArrayList<>();
        }
        
        return ret;
    }
    
    /**
     * Gets all external parents of a child by child id
     * @param childId id of child to get parents of
     * @return all external parents of child (empty if none)
     */
    public List<EtyExternalParent> getWordExternalParents(Integer childId) {
        return childToExtParent.containsKey(childId) ? 
                new ArrayList<EtyExternalParent>(childToExtParent.get(childId).values()) : 
                new ArrayList<EtyExternalParent>();
    }
    
    /**
     * Gets all parent ids of child word by child id
     * @param childId id of child to query for parents
     * @return list of parent ids (empty if none)
     */
    public List<Integer> getWordParentsIds(Integer childId) {
        return childToParent.containsKey(childId) ? childToParent.get(childId) 
                : new ArrayList<Integer>();
    }
    
    /**
     * Sets relation of external parent to child word
     * NOTE: USE UNIQUE ID OF PARENT RATHER THAN SIMPLE VALUE
     * @param parent Full unique ID of external parent
     * @param child child's ID
     */
    public void addExternalRelation(EtyExternalParent parent, Integer child) {
        // return immediately if child does not exist
        if (core.getWordCollection().exists(child)) {
            if (!extParentToChild.containsKey(parent.getUniqueId())) {
                List<Integer> myList = new ArrayList<>();
                myList.add(child);
                extParentToChild.put(parent.getUniqueId(), myList);
            } else {
                List<Integer> myList = extParentToChild.get(parent.getUniqueId());
                if (!myList.contains(child)) {
                    myList.add(child);
                }
            }
            
            if (!childToExtParent.containsKey(child)) {
                Map<String, EtyExternalParent> myMap = new HashMap<>();
                myMap.put(parent.getUniqueId(), parent);
                childToExtParent.put(child, myMap);
            } else {
                Map<String, EtyExternalParent> myMap = childToExtParent.get(child);
                if (!myMap.containsKey(parent.getUniqueId())) {
                    myMap.put(parent.getUniqueId(), parent);
                }
            }
        }
    }
    
    public void delExternalRelation(EtyExternalParent parent, Integer child) {
        // only run if child exists
        if (core.getWordCollection().exists(child)) {
            if (extParentToChild.containsKey(parent.getUniqueId())) {
                List<Integer> myList = extParentToChild.get(parent.getUniqueId());
                if (myList.contains(child)) {
                    myList.remove(child);
                }
                
                if (myList.isEmpty()) {
                    allExtParents.remove(getExtListParentValue(parent));
                }
            }
            
            if (childToExtParent.containsKey(child)) {
                Map<String, EtyExternalParent> myMap = childToExtParent.get(child);
                
                if (myMap.containsKey(parent.getUniqueId())) {
                    myMap.remove(parent.getUniqueId());
                }
            }
        }
    }
    
    /**
     * Add external parent to total list if it does not already exist.
     * No corrolary to remove, as this is regenerated at every load. Old 
     * values will fall away at this point.
     * @param parent Parent to add to list.
     */
    private void addExtParentToList(EtyExternalParent parent) {
        String parentValue = getExtListParentValue(parent);
        if (!allExtParents.contains(parentValue)) {
            allExtParents.add(parentValue);
        }
        
        Collections.sort(allExtParents);
    }
    
    /**
     * Creates external parent display value (used as ID for list of all external
     * parents for use in filtering
     * @param parent
     * @return 
     */
    private String getExtListParentValue(EtyExternalParent parent) {
        // TODO: REVISIT THIS: NEED TO USE ACTUAL OBJECT IN LIST TO ALLOW FOR FILTERING
        return parent.getExternalWord() + " (" + parent.getExternalLanguage() + ")";
    }
    
    /**
     * Gets list of every external parent referenced in entire language
     * @return alphabetical list by word + (language)
     */
    private List<String> getExtParentList() {
        return allExtParents;
    }
    
    /**
     * Deletes relationship between parent and child if one exists
     * @param parentId
     * @param childId 
     */
    public void delRelation(Integer parentId, Integer childId) {
        if (parentToChild.containsKey(parentId)) {
            List<Integer> myList = parentToChild.get(parentId);
            
            if (myList.contains(childId)) {
                myList.remove(childId);
            }
        }
        
        if (childToParent.containsKey(childId)) {
            List<Integer> myList = childToParent.get(childId);
            if (myList.contains(parentId)) {
                myList.remove(parentId);
            }
        }
    }
    
    /**
     * Writes all word information to XML document
     *
     * @param doc Document to write to
     * @param rootElement root element of document
     */
    public void writeXML(Document doc, Element rootElement) {
        ConWordCollection wordCollection = core.getWordCollection();
        Element collection = doc.createElement(PGTUtil.EtyCollectionXID);
        
        // we only need to record the relationship one way, the bidirection will be regenerated
        for (Entry<Integer, List<Integer>> curEntry : parentToChild.entrySet()) {
            // skip nonexistant words
            if (!wordCollection.exists(curEntry.getKey())) {
                continue;
            }
            
            Element myNode = doc.createElement(PGTUtil.EtyIntRelationNodeXID);
            myNode.appendChild(doc.createTextNode(curEntry.getKey().toString()));
            
            for (Integer curChild : curEntry.getValue()) {
                if (!wordCollection.exists(curChild)) {
                    continue;
                }
                
                Element child = doc.createElement(PGTUtil.EtyIntChildXID);
                child.appendChild(doc.createTextNode(curChild.toString()));
                myNode.appendChild(child);
            }
            collection.appendChild(myNode);
        }
        
        // adds a node for each word with at least one external parent
        for (Entry<Integer, Map<String, EtyExternalParent>> curEntry 
                : childToExtParent.entrySet()) {
            Element childContainer = doc.createElement(PGTUtil.EtyChildExternalsXID);
            childContainer.appendChild(doc.createTextNode(curEntry.getKey().toString()));
            
            // creates a node for each external parent within a word
            for (EtyExternalParent curParent : curEntry.getValue().values()) {
                Element extParentNode = doc.createElement(PGTUtil.EtyExternalWordNodeXID);
                // record external word value
                Element curElement = doc.createElement(PGTUtil.EtyExternalWordValueXID);
                curElement.appendChild(doc.createTextNode(curParent.getExternalWord()));
                extParentNode.appendChild(curElement);
                // record external word origin
                curElement = doc.createElement(PGTUtil.EtyExternalWordOriginXID);
                curElement.appendChild(doc.createTextNode(curParent.getExternalLanguage()));
                extParentNode.appendChild(curElement);
                // record external word definition
                curElement = doc.createElement(PGTUtil.EtyExternalWordDefinitionXID);
                curElement.appendChild(doc.createTextNode(curParent.getDefinition()));
                extParentNode.appendChild(curElement);
                
                childContainer.appendChild(extParentNode);
            }
            collection.appendChild(childContainer);
        }
        
        rootElement.appendChild(collection);
    }
    
    /**
     * Tests whether adding a parent-child relationship would create an illegal
     * looping scenario
     * @param parentId parent word ID to check
     * @param childId child word ID to check
     * @return true if illegal due to loop, false otherwise
     */
    private boolean createsLoop(Integer parentId, Integer childId) {
        return parentId.equals(childId) || createsLoopParent(parentId, childId)
                || createsLoopChild(parentId, childId);
    }
    
    /**
     * Tests whether a child->parent addition creates an illegal loop.
     * Recursive.
     * @param parentId current value to check against (begin with self)
     * @param childId bottommost child ID being checked
     * @return true if illegal due to loop, false otherwise
     */
    private boolean createsLoopParent(Integer curWordId, Integer childId) {
        boolean ret = false;
        
        if (childToParent.containsKey(curWordId)) {
            for (Integer selectedParent : childToParent.get(curWordId)) {
                ret = selectedParent.equals(childId) 
                        || createsLoopParent(selectedParent, childId);
                
                // break on single loop occurance and return
                if (ret) {
                    break;
                }
            }
        }
           
        return ret;
    }
    
    /**
     * Tests whether a parent->child addition creates an illegal loop.
     * Recursive.
     * @param parentId topmost parent ID to check against
     * @param curWordId ID of current word being checked against
     * @return true if illegal due to loop, false otherwise
     */
    private boolean createsLoopChild(Integer parentId, Integer curWordId) {
        boolean ret = false;
        
        // test base parent ID against all children of current word
        // and of all subsequent children down the chain
        for (Integer childId : this.getChildren(curWordId)) {
            ret = parentId.equals(childId) && createsLoopChild(parentId, childId);
            
            // break on single loop occurance and return
            if (ret) {
                break;
            }
        }
        
        return ret;
    }
    
    public void setBufferParent(Integer _bufferParent) {
        bufferParent = _bufferParent;
    }
    
    public void setBufferChild(Integer _bufferChild) {
        bufferChild = _bufferChild;
    }
    
    public EtyExternalParent getBufferExtParent() {
        return bufferExtParent;
    }
    
    public void insertBufferExtParent() {
        addExternalRelation(bufferExtParent, bufferChild);
        bufferExtParent = new EtyExternalParent();
    }
    
    /**
     * Inserts buffer values and clears buffer
     */
    public void insert() {
        try {
            addRelation(bufferParent, bufferChild);
            // Do NOT set these to 0. This relies on the parent buffer persisting.
        } catch (IllegalLoopException ex) {
            // do nothing. These will have been eliminated at the time of archiving.
        }
    }
    
    public class IllegalLoopException extends Exception {
        public IllegalLoopException(String message) {
            super(message);
        }
    }
}
