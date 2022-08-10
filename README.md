EHRMiddleware - The InteropEHRate project (www.interopEHRate.eu)

This project implements the EHRMiddleware a service provided by
an Healthcare Organization (HCO) to enable the integration between
an instance of R2DAccess Server and the EHR of the HCO.

The EHRMiddleware is a service that allows to access to the EHR of an HCO
using the HL7/FHIR RESTful API. A running instance of EHRMiddleware represents
the FHIR RESTFul interface of a EHR.

The current version of the EHRMiddleware implements a small subset of the whole
FHIR Restful APIs specification (it implements only the operations used during
the execution of the Pilot Applications of the project). This is the list of
the provided operations (they are all executed in the compartment of the
authenticated citizen):
1) Search of Encounter
2) Operation Encounter$everything
3) Operation (non standard) Patient$patient-summary

Despite the technology used by the real EHR, the current EHRMiddleware
integrates to the EHR using the RESTFul model.

The EHRMiddleware is not only responsible to forwards incoming call made using
the FHIR RESTFul API to concrete calls made to the EHR using the integration
APIs provided by the EHR, but it is also responsible to use an additional
service called IHS used to convert data provided by the EHR from their native format
to the FHIR data model (compliant with the InteropEHRate Interoperability profiles).

In order to process every incoming request the EHRMiddleware executes the
following workflow:
  1) Identify the citizen as a patient of the EHR (the citizen authenticates
     through eIDAS, he / she does not use an identity platform provided by the HCO).
  2) Download (and save to file system) the requested health data from the EHR
  3) Extract large images from the received files
  4) Create anonymized version of the images
  5) Request conversion of health data downloaded from the EHR to IHS
  6) Download from IHS (and store to file system) the converted JSON/FHIR file 
  7) Notify the R2DAccess Server about the request completion.

The EHRMiddleware uses an internal Workflow Engine, composed by several Work
classes each of which controls a specific activity of the workflow. Eack Work
class uses a specific service class to execute the concrete activity. 

The Spring configuration file of the EHRMiddleware defines what are the 
concrete beans to instantiate to execute the workflow. These beans are the 
customized classes used to access the EHR of an HCO, in details these are 
classes that implement the interface eu.interopehrate.r2d.ehr.services.EHRService. 
Most likely these are the only classes that needs to be customized in order to 
integrate the EHRMiddleware to a EHR.
