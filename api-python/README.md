# SEPA Python API

Client-side libraries for the SEPA platform.

## Installation

Install the published package:

```
pip install sepy
```

Or install it from this monorepo checkout:

```
cd api-python
pip install .
```

Build the source distribution and wheel locally:

```
cd api-python
python -m pip install build
python -m build
```

## Usage

To use the classes you have to import them in this way:
```
from sepy.<the class you want to import> import *
```

For example, if you want to import the SAPObject (used to handle JSAP files) 
you have to write:
```python3
from sepy.SAPObject import *
```

This library consists of five modules that can be used for different purposes:

- SAPObject: A handler class for SAP files
- SEPA: A low-level class used to develop a client for SEPA
- ConnectionHandler: A class for connection handling
- Exceptions
- tablaze: A runnable script (also callable as a function, to nicely print SEPA output)

Let's talk about some classes deeply:

## SEPA

These APIs allow to develop a client for the SEPA platform using a simple interface. 
First of all the class SEPA must be initialized. Then the standard methods 
to interact with the broker are available.

### Parameters:
- sapObject :
  A SAPObject file Default = None
- logLevel :
  A number indicating the desired log level. Default = 40
The parameters are optional. If present, they activate query, update, subscribe, 
methods by SAPObject pick. If absent, only the equivalnt `sparql_*` methods 
are available, giving the host communication information each time.

### Attributes:
- logger
- sap:
  the SAPObject
- connectionManager :
  The underlying responsible for network connections

### Creating a SEPA client

```python3
mySAP = open(path_to_sap,"r")
sap = SAPObject(yaml.load(mySAP))
sc = SEPA(sapObject=sap)
```

### Query and Update

These four methods (`query`, `sparql_query`, `update`, `sparql_update`, 
`query_all`, `clear`) expect either a sap entry or a SPARQL query/update. 
In addition, it is possible to overwrite the sap communication parameters 
with sepa. When a new query/update is issued, it may be preferrable to 
catch the `RegistrationFailedExceptions`, `TokenExpiredException` and 
`TokenRequestFailedException` errors. The query methods return the SEPA answer.

### Subscribe and Unsubscribe

The `subscribe` and `sparql_subscribe` primitive requires a sap entry or 
a SPARQL query, an alias for the subscription, an handler (a lambda expression
or a method with two parameters, one for added, the other for removed) 
and if needed the overwriting params for communication. 
The `unsubscribe` primitive only needs to know the ID of the subscription.

## SAPObject

This package supports Semantic Application Profiles. The package is encoding
free, since it expects a dictionary in input. Therefore, for a ysap we have
```
mySAP = open(path_to_sap,"r")
sap = SAPObject(yaml.load(mySAP))
```
while for a jsap we have
```
mySAP = open(path_to_sap,"r")
sap = SAPObject(json.load(mySAP))
```

## Something else?

Documentation is being written...

## Foreseen changes
Minors, plus the addition of some utilities.
