/*
 * Copyright (c) 2014, Draque Thompson
 * All rights reserved.
 *
 * Licensed under: Creative Commons Attribution-NonCommercial 4.0 International Public License
 * See LICENSE.TXT included with this code to read the full license agreement.
 *
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
package PolyGlot;

import java.awt.Color;
import java.awt.Font;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class DictCore {

    private final String version = "0.9.2";
    private final ConWordCollection wordCollection = new ConWordCollection(this);
    private final TypeCollection typeCollection = new TypeCollection();
    private final GenderCollection genderCollection = new GenderCollection(this);
    private final DeclensionManager declensionMgr = new DeclensionManager();
    private final PropertiesManager propertiesManager = new PropertiesManager();
    private final PronunciationMgr pronuncMgr = new PronunciationMgr(this);
    private final ThesaurusManager thesManager = new ThesaurusManager(this);

    /**
     * Gets proper color for fields marked as required
     * @return 
     */
    public Color getRequiredColor() {
        return new Color(255,204,204);
    }
    
    /**
     * gets thesaurus manager
     *
     * @return ThesaurusManager object from core
     */
    public ThesaurusManager getThesManager() {
        return thesManager;
    }

    /**
     * gets properties manager
     *
     * @return PropertiesManager object from core
     */
    public PropertiesManager getPropertiesManager() {
        return propertiesManager;
    }

    /**
     * gets version ID of PolyGlot
     *
     * @return String value of version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets lexicon manager
     *
     * @return ConWordCollection from core
     */
    public ConWordCollection getWordCollection() {
        return wordCollection;
    }

    /**
     * Builds a report on the conlang. Potentially very computationally
     * expensive.
     *
     * @return String formatted report
     */
    public String buildLanguageReport() {
        String ret = "<center>---LANGUAGE STAT REPORT---</center><br><br>";

        ret += propertiesManager.buildPropertiesReport();

        ret += wordCollection.buildWordReport();

        return ret;
    }

    public DictCore() {
        Map alphaOrder = propertiesManager.getAlphaOrder();

        wordCollection.setAlphaOrder(alphaOrder);
        typeCollection.setAlphaOrder(alphaOrder);
        genderCollection.setAlphaOrder(alphaOrder);
    }

    /**
     * Reads from given file
     *
     * @param _fileName filename to read from
     * @throws Exception
     */
    public void readFile(String _fileName) throws Exception {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            CustHandler handler = CustHandlerFactory.getCustHandler(IOHandler.getDictFile(_fileName), this);

            handler.setWordCollection(wordCollection);
            handler.setTypeCollection(typeCollection);

            saxParser.parse(IOHandler.getDictFile(_fileName), handler);
            
            Font conFont = IOHandler.getFontFrom(_fileName);
            if (conFont != null) {
                propertiesManager.setFontCon(conFont);
            }
        } catch (ParserConfigurationException e) {
            throw new Exception(e.getMessage());
        } catch (SAXException e) {
            throw new Exception(e.getMessage());
        } catch (IOException e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Writes to given file
     *
     * @param _fileName filename to write to
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws javax.xml.transform.TransformerException
     * @throws java.io.FileNotFoundException
     */
    public void writeFile(String _fileName)
            throws ParserConfigurationException, TransformerException, FileNotFoundException, IOException {        
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Element wordValue;

        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement(XMLIDs.dictionaryXID);
        doc.appendChild(rootElement);

        // store version of PolyGlot
        wordValue = doc.createElement(XMLIDs.pgVersionXID);
        wordValue.appendChild(doc.createTextNode(version));
        rootElement.appendChild(wordValue);

        // collect XML representation of all dictionary elements
        propertiesManager.writeXML(doc, rootElement);
        genderCollection.writeXML(doc, rootElement);
        typeCollection.writeXML(doc, rootElement);
        wordCollection.writeXML(doc, rootElement);
        declensionMgr.writeXML(doc, rootElement);
        pronuncMgr.writeXML(doc, rootElement);

        // write thesaurus entries
        rootElement.appendChild(thesManager.writeToSaveXML(doc));
        
        // have IOHandler write constructed document to file
        IOHandler.writeFile(_fileName, doc, this);
    }

    /**
     * deletes word based on word ID
     * Makes sure to clear all records of word declension
     * @param _id
     * @throws java.lang.Exception
     */
    public void deleteWordById(Integer _id) throws Exception {
        wordCollection.deleteNodeById(_id);
        clearAllDeclensionsWord(_id);
    }

    /**
     * Clears all declensions from word
     *
     * @param wordId ID of word to clear of all declensions
     */
    public void clearAllDeclensionsWord(Integer wordId) {
        declensionMgr.clearAllDeclensionsWord(wordId);
    }

    public DeclensionNode getDeclensionTemplate(Integer typeId, Integer templateId) {
        return declensionMgr.getDeclensionTemplate(typeId, templateId);
    }

    public DeclensionManager getDeclensionManager() {
        return declensionMgr;
    }

    /**
     * Checks whether word is legal and returns error reason if not
     *
     * @param word word to check legality of
     * @return String of error is illegal, empty string otherwise (returns first
     * problem)
     */
    public String isWordLegal(ConWord word) {
        String ret = "";

        if (word.getValue().equals("")) {
            ret = "Words must have a cownword value set.";
        } else if (word.getWordType().equals("") && propertiesManager.isTypesMandatory()) {
            ret = "Types set to mandatory; please fill in type.";
        } else if (word.getLocalWord().equals("") && propertiesManager.isLocalMandatory()) {
            ret = "Local word set to mandatory; please fill in local word.";
        } else if (propertiesManager.isWordUniqueness() && wordCollection.containsWord(word.getValue())) {
            ret = "ConWords set to enforced unique, plese select spelling without existing homonyms.";
        } else if (propertiesManager.isLocalUniqueness() && !word.getLocalWord().equals("")
                && wordCollection.containsLocalMultiples(word.getLocalWord())) {
            ret = "Local words set to enforced unique, and this local exists elsewhere.";
        }

        // for more complex checks, use this pattern, only checking if other problems do not exist
        if (ret.equals("")) {
            ret = typeCollection.typeRequirementsMet(word);
        }

        if (ret.equals("")) {
            ret = declensionMgr.declensionRequirementsMet(word, typeCollection.findTypeByName(word.getWordType()));
        }

        return ret;
    }

    /**
     * Safely modify a type (updates words of this type automatically)
     *
     * @param id type id
     * @param modType new type
     * @throws Exception
     */
    public void modifyType(Integer id, TypeNode modType) throws Exception {
        Iterator<ConWord> it;
        ConWord typeWord = new ConWord();

        typeWord.setWordType(typeCollection.getNodeById(id).getValue());
        typeWord.setValue("");
        typeWord.setDefinition("");
        typeWord.setGender("");
        typeWord.setLocalWord("");
        typeWord.setPronunciation("");

        it = wordCollection.filteredList(typeWord);

        while (it.hasNext()) {
            ConWord modWord = it.next();

            modWord.setWordType(modType.getValue());

            wordCollection.modifyNode(modWord.getId(), modWord);
        }

        typeCollection.modifyNode(id, modType);
    }

    public TypeCollection getTypes() {
        return typeCollection;
    }

    public GenderCollection getGenders() {
        return genderCollection;
    }

    public PronunciationMgr getPronunciationMgr() {
        return pronuncMgr;
    }
}
