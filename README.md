# fhirfederator

This project aims to build a federated database management system for FHIR servers.

For more explanations about federated databases, see __https://en.wikipedia.org/wiki/Federated_database_system__

These features are supported:

* Execute queries against multiple FHIR databases
* Each FHIR resource can be looked for in one or multiple FHIR databases
* Equality between resources is determined by one or more common identifiers. Resources without identifier will never be equal to another resource.

There are many features and configuration options to add...

if you want to contribute to the functional specs of this server, please add your questions here: __https://docs.google.com/document/d/1l9ioxTjjpgxCLXGkgLkw3_Qlvh8Tc9E6GalX9XMzgBE/edit?usp=sharing__

Configuration is now done in __https://github.com/bartdecu/fhirfederator/blob/main/src/main/resources/application.yml__
