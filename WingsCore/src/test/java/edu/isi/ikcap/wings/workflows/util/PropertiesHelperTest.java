package edu.isi.ikcap.wings.workflows.util;

import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 *
 */
public class PropertiesHelperTest {

  /**
   * a logger for this class
   */
  private static Logger logger = LoggerFactory.getLogger(PropertiesHelperTest.class);

  private final String PATH_TO_ARCHIVE = "./src/test/resources/";

  private final String PATH_TO_DM_PROPERTIES = PATH_TO_ARCHIVE + "wings-dm-domain.properties";

  private final String PATH_TO_GPCLA_PROPERTIES = PATH_TO_ARCHIVE + "wngs-gpcla-domain.properties";

  private static final String HNBMODELER = "HNBModeler";

  @SuppressWarnings("unused")
  private static final String JAVA_MAX_HEAP_SIZE = "javaMaxHeapSize";
  
  @SuppressWarnings("unused")
  private static final String CONSENSUS = "Consensus";
  
  @SuppressWarnings("unused")
  private static final String CLUSTER_BY = "clusterBy";

  @Before
  public void setUp() {
    // Add your code here
  }

  @After
  public void tearDown() {
    // Add your code here
  }

  public ComponentVariable findComponent(String componentName, ComponentCatalog catalog) {
    logger.debug("called with:  componentName = {}", componentName);
    ArrayList<ComponentVariable> components = catalog.getAllComponentTypes();
    ComponentVariable result = null;
    for (ComponentVariable component : components) {
      String componentTypeName = component.getComponentTypeName();
      //System.out.println("componentTypeName = " + componentTypeName);
      if (componentTypeName.equals(componentName)) {
        result = component;
        result.setID(componentName);
      }
    }
    if (result == null) {
      logger.error("findComponent is returning null");
    } else {
      logger.debug("is returning {}", result);
    }
    return result;
  }

  @Test
  public void testFindComponent() {
    ComponentCatalog pc = PropertiesHelper.getPCFactory().getPC(PropertiesHelper.getDCDomain(),
      PropertiesHelper.getPCDomain(), null);
    ComponentVariable result = findComponent(HNBMODELER, pc);
    Assert.assertNotNull(result);
  }


  @Test
  public void testCreateDir() {
    // Add your code here
  }

  @Test
  public void testGetProposedLogFileName() {
    // Add your code here
  }

  // @Test
  public void testLoadWingsProperties() {
//    String pathToDmDomain = "./src/test/resources/wings-dm-domain.properties";
//    PropertiesHelper.loadWingsProperties(pathToDmDomain);
//    ComponentCatalog pc = PropertiesHelper.getPCFactory().getPC(PropertiesHelper.getDCDomain(),
//      PropertiesHelper.getPCDomain(), null);
//    ArrayList<ComponentVariable> components = pc.getAllComponentTypes();
//    for (ComponentVariable component : components) {
//      System.out.println("component.getComponentTypeName() = " + component.getComponentTypeName());
//    }

    PropertiesHelper.loadWingsProperties(PATH_TO_DM_PROPERTIES);
    ComponentCatalog pc = PropertiesHelper.getPCFactory().getPC(PropertiesHelper.getDCDomain(),
      PropertiesHelper.getPCDomain(), null);
    ComponentVariable result = findComponent(HNBMODELER, pc);
    Assert.assertNotNull(result);
    String ontologyDomain = PropertiesHelper.getOntologyDir();
    //System.out.println("ontologyDomain = " + ontologyDomain);

    PropertiesHelper.loadWingsProperties(PATH_TO_GPCLA_PROPERTIES);
    PropertiesHelper.setDomainDir("/moody/Dropbox/ikcap/Wings/trunk/ontology/GPDomain");
    String pcDomain = PropertiesHelper.getPCDomain();
    System.out.println("dcDomain = " + pcDomain);
    ontologyDomain = PropertiesHelper.getOntologyDir();
    System.out.println("ontologyDomain = " + ontologyDomain);

    PropertiesHelper.getPCFactory();
    String pcDir = PropertiesHelper.getPCDir();
    System.out.println("pcDir = " + pcDir);
    String url = PropertiesHelper.getPCDomainURL();
    System.out.println("url = " + url);
//    pc = PropertiesHelper.getPCFactory().getPC(PropertiesHelper.getDCDomain(),
//      PropertiesHelper.getPCDomain(), null);

    result = findComponent("Consensus", pc);
    Assert.assertNotNull(result);
  }

  @Test
  public void testGetPCDomain() {
    // Add your code here
  }

  @Test
  public void testGetDCDomain() {
    // Add your code here
  }

  @Test
  public void testGetTemplateDomain() {
    // Add your code here
  }

  @Test
  public void testGetGraphvizPath() {
    // Add your code here
  }

  @Test
  public void testSetLogDir() {
    // Add your code here
  }

  @Test
  public void testGetLogDir() {
    // Add your code here
  }

  @Test
  public void testSetOutputDir() {
    // Add your code here
  }

  @Test
  public void testGetOutputDir() {
    // Add your code here
  }

  @Test
  public void testSetResourceDir() {
    // Add your code here
  }

  @Test
  public void testGetResourceDir() {
    // Add your code here
  }

  @Test
  public void testGetOntologyDir() {
    // Add your code here
  }

  @Test
  public void testSetOntologyDir() {
    // Add your code here
  }

  @Test
  public void testGetOntologyURL() {
    // Add your code here
  }

  @Test
  public void testGetWorkflowOntologyPath() {
    // Add your code here
  }

  @Test
  public void testGetPCFactory() {
    ComponentCatalog pc = PropertiesHelper.getPCFactory().getPC(PropertiesHelper.getDCDomain(),
      PropertiesHelper.getPCDomain(), null);
    ArrayList<ComponentVariable> components = pc.getAllComponentTypes();
    ComponentVariable result = null;
    for (ComponentVariable component : components) {
      String compomentType = component.getComponentType();
      System.out.println("compomentType = " + compomentType);
      String componentTypeName = component.getComponentTypeName();
      System.out.println("componentTypeName = " + componentTypeName);
      //String typeName = component.getTypeName();
      //System.out.println("typeName = " + typeName);
//      if (componentTypeName.equals(componentName)) {
//        result = component;
//        result.setID(componentName);
//      }
    }
  }

  @Test
  public void testGetDCFactory() {
    // Add your code here
  }

  @Test
  public void testGetProvenanceFlag() {
    // Add your code here
  }

  @Test
  public void testGetTrimmingNumber() {
    // Add your code here
  }

  @Test
  public void testGetOutputFormat() {
    // Add your code here
  }

  @Test
  public void testGetDCPrefixNSMap() {
    // Add your code here
  }

  @Test
  public void testGetPCPrefixNSMap() {
    // Add your code here
  }

  @Test
  public void testGetDCPropertyForCurrentDomain() {
    // Add your code here
  }

  @Test
  public void testGetPCPropertyForCurrentDomain() {
    // Add your code here
  }

  @Test
  public void testGetPCNewComponentPrefix() {
    // Add your code here
  }

  @Test
  public void testGetDCNewDataPrefix() {
    // Add your code here
  }

  @Test
  public void testGetQueryNamespace() {
    // Add your code here
  }

  @Test
  public void testGetQueryVariablesNamespace() {
    // Add your code here
  }
}
