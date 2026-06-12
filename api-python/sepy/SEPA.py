#!/usr/bin/python3
# -*- coding: utf-8 -*-
#
#  SEPA.py
#  
#  Copyright 2018 Francesco Antoniazzi <francesco.antoniazzi1991@gmail.com>
#  
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License, or
#  (at your option) any later version.
#  
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#  
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
#  MA 02110-1301, USA.
#  
#  

from .SAPObject import SAPObject
from .ConnectionHandler import *

from urllib.parse import urlparse

import logging
import json


class SEPA:
    def __init__(self, sapObject=None, client_id=None, logLevel=logging.ERROR):
        """
        Constructor for SEPA engine representation.
        'sapObject' must be given, to use update, query, subscribe functions.
        'client_id
        """
        # logger configuration
        self.logger = logging.getLogger("sepaLogger")
        self.logger.setLevel(logLevel)
        logging.basicConfig(format='%(levelname)s:%(message)s', level=logLevel)
        self.logger.debug("=== SEPA::__init__ invoked ===")

        # initialize data structures
        self.sap = sapObject
        self.connectionManager = ConnectionHandler(client_id=client_id, logLevel=logLevel)
    
    def get_client_id(self):
        """
        Retrieves the current client_id.
        """
        return self.connectionManager.get_client_id()

    def get_subscriptions(self):
        """
        Getter for subscriptions currently opened, in a dict form
        """
        return self.connectionManager.get_subscriptions()

    def setSAP(self, sapObject):
        """
        Setter of sapObject at runtime.
        """
        self.sap = sapObject

    def query(self, sapIdentifier, forcedBindings={}, destination=None,
              host=None, token_url=None, register_url=None):
        """
        Performs a query with the sap entry tag 'sapIdentifier';
        'forcedBindings' can be given as dict form for substitution.
        If you want to store the output of the query in a file, use the
        'destination' field to give the path.
        'host', 'token_url' and 'register_url' can be given to overwrite
        the sap values (if any).
        Returns the output of the query.
        """
        sparql = self.sap.getQuery(sapIdentifier, forcedBindings)
        return self.sparql_query(
            sparql, destination=destination,
            host=host, token_url=token_url, register_url=register_url)

    def sparql_query(self, sparql, destination=None, host=None,
                     token_url=None, register_url=None):
        """
        Performs a query with the plain sparql;
        If you want to store the output of the query in a file, use the
        'destination' field to give the path.
        'host', 'token_url' and 'register_url' can be given to overwrite
        the sap values (if any).
        Returns the output of the query.
        """
        # perform the query request
        if self.sap is None and host is None:
            raise ValueError("Host parametrization is necessary if no SAPObject is given to SEPA instance")
        sepa_host = self.sap.query_url if (host is None) else host
        protocol = urlparse(sepa_host).scheme
        if protocol == "https":
            if self.sap is None and (token_url is None or register_url is None):
                raise ValueError("Token and Register URL must not be None if no SAPObject is given to SEPA instance")
            sepa_token = self.sap.tokenRequest_url if (token_url is None) else token_url
            sepa_register = self.sap.registration_url if (register_url is None) else register_url
            status, results = self.connectionManager.secureRequest(
                sepa_host, sparql, True, sepa_token, sepa_register)
        elif protocol == "http":
            status, results = self.connectionManager.unsecureRequest(
                sepa_host, sparql, True)
        else:
            raise NotImplementedError("Still only http, https, ws, wss protocols are implemented")
        if int(status) == 200:
            jresults = json.loads(results)
            if "error" in jresults:
                self.logger.error(jresults["error"]["message"])
                raise ValueError(error_message)
            elif destination is not None:
                with open(destination, "w") as fileDest:
                    print(json.dumps(jresults), file=fileDest)
            return jresults
        else:
            error_message = "Query status code: {}".format(status)
            self.logger.error(error_message)
            raise ValueError(error_message)

    def update(self, sapIdentifier, forcedBindings={},
               host=None, token_url=None, register_url=None):
        """
        Performs an update with the sap entry tag 'sapIdentifier';
        'forcedBindings' can be given as dict form for substitution.
        'host', 'token_url' and 'register_url' can be given to overwrite
        the sap values (if any).
        """
        sparql = self.sap.getUpdate(sapIdentifier, forcedBindings)
        return self.sparql_update(sparql, host=host, token_url=token_url,
                                  register_url=register_url)

    def sparql_update(self, sparql, host=None, token_url=None, register_url=None):
        """
        Performs an update with the plain sparql;
        'host', 'token_url' and 'register_url' can be given to overwrite
        the sap values (if any).
        """
        # perform the update request
        if self.sap is None and host is None:
            raise ValueError("Host parametrization is necessary if no SAPObject is given to SEPA instance")
        sepa_host = self.sap.update_url if (host is None) else host
        protocol = urlparse(sepa_host).scheme
        
        if protocol == "https":
            if self.sap is None and (token_url is None or register_url is None):
                raise ValueError("Token and Register URL must not be None if no SAPObject is given to SEPA instance")
            sepa_token = self.sap.tokenRequest_url if (token_url is None) else token_url
            sepa_register = self.sap.registration_url if (register_url is None) else register_url
            status, results = self.connectionManager.secureRequest(
                sepa_host, sparql, False, sepa_register, sepa_token)
        elif protocol == "http":
            status, results = self.connectionManager.unsecureRequest(
                sepa_host, sparql, False)
        else:
            raise NotImplementedError("Still only http, https, ws, wss protocols are implemented")
        # return
        if int(status) == 200:
            return results
        else:
            self.logger.error(results)
            raise ValueError(results)

    def clear(self, host=None, token_url=None, register_url=None):
        """
        Performs a simple 'delete where {?a ?b ?c}'.
        'host', 'token_url' and 'register_url' can be given to overwrite
        the sap values (if any).
        """
        return self.sparql_update(
            "delete where {?a ?b ?c}", host=host,
            token_url=token_url, register_url=register_url)

    def query_all(self, destination=None, host=None,
                  token_url=None, register_url=None):
        """
        Performs a 'select * where {?a ?b ?c}'
        If you want to store the output of the query in a file, use the
        'destination' field to give the path.
        'host', 'token_url' and 'register_url' can be given to overwrite
        the sap values (if any).
        Returns the output of the query
        """
        return self.sparql_query(
            "select * where {?a ?b ?c}", destination=destination,
            host=host, token_url=token_url, register_url=register_url)

    def sparql_subscribe(self, sparql, alias, handler=lambda a, r: None,
                         host=None, token_url=None, register_url=None,
                         default_graph=None, named_graph=None):
        """
        Subscribes to a specific 'sparql'. The subscription will have its
        own 'alias'. A 'handler' to be triggered when the subscription starts
        can be give.
        'host', 'token_url', 'register_url', 'default_graph' and 'named_graph'
        can be given to overwrite the sap values (if any).
        Returns the subscription id.
        """
        subid = None
        if self.sap is None and host is None:
            raise ValueError("Host parametrization is necessary if no SAPObject is given to SEPA instance")
        sepa_host = self.sap.subscribe_url if (host is None) else host
        protocol = urlparse(sepa_host).scheme
        
        def_graph = None
        if default_graph is not None:
            def_graph = default_graph
        elif self.sap is not None and "default-graph-uri" in self.sap.graphs.keys():
            def_graph = self.sap.graphs["default-graph-uri"]
        nam_graph = None
        if named_graph is not None:
            nam_graph = default_graph
        elif self.sap is not None and "named-graph-uri" in self.sap.graphs.keys():
            nam_graph = self.sap.graphs["named-graph-uri"]
        
        if protocol == "wss":
            if self.sap is None and (token_url is None or register_url is None):
                raise ValueError("Token and Register URL must not be None if no SAPObject is given to SEPA instance")
            sepa_token = self.sap.tokenRequest_url if (token_url is None) else token_url
            sepa_register = self.sap.registration_url if (register_url is None) else register_url
            subid = self.connectionManager.openSecureWebsocket(
                sepa_host, sparql, alias, handler, sepa_register, sepa_token,
                default_graph=def_graph, named_graph=nam_graph)
        elif protocol == "ws":
            subid = self.connectionManager.openUnsecureWebsocket(
                sepa_host, sparql, alias, handler, default_graph=def_graph,
                named_graph=nam_graph)
        else:
            raise NotImplementedError("Still only http, https, ws, wss protocols are implemented")
        return subid

    def subscribe(self, sapIdentifier, alias, forcedBindings={},
                  handler=lambda a, r: None,
                  host=None, token_url=None, register_url=None,
                  default_graph=None, named_graph=None):
        """
        Performs a subscription with the sap identifier tag and its
        forcedBindings; an 'alias' has to be given to the subscription,
        as well as an handler to be called upon notification.
        'host', 'token_url', 'register_url', 'default_graph' and 'named_graph'
        can be given to overwrite the sap values (if any).
        The subscription id is returned.
        """
        sparql = self.sap.getQuery(sapIdentifier, forcedBindings)
        return self.sparql_subscribe(
            sparql, alias, handler, host=host,
            token_url=token_url, register_url=register_url,
            default_graph=default_graph, named_graph=named_graph)

    def unsubscribe(self, subid):
        """
        Closes the subscription, given the subscription id
        """
        self.connectionManager.closeWebsocket(subid)
