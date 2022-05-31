# Requirements document for a FHIR federation server

## R0. General

* R0.1 The functional identity of a resource is determined by a business identifier, not by a technical id
* R0.2 If a resource is present on multiple servers, a merging strategy should be defined

## R1. Support Search

* R1.1 Queries should be interpreted by the federation server in such a way that each resource can be on a different FHIR server
* R1.2 The federation server should be able to execute against one or many FHIR servers. One resource can be one or many servers, and multiple resources can be on one server.
* R1.3 The result of a query to a federated server should be one searchset. On the structural level, the result of a query to a single FHIR server should be identical to the result of a query to a federated server.

## R2. Support Create

* R2.1 ...
