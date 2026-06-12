#!/usr/bin/python3
# -*- coding: utf-8 -*-
#
#  ConnectionHandler.py
#  
#  Copyright 2018   Francesco Antoniazzi <francesco.antoniazzi1991@gmail.com>,
#                   Fabio Viola <desmovalvo@gmail.com>
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

import requests
import logging
import json
import sys

from ssl import CERT_NONE
from websocket import WebSocketApp
from base64 import b64encode
from time import sleep
from threading import Thread, Event
from uuid import uuid4
from .Exceptions import *


REGISTER_PAYLOAD = """{{ "register": {{ "client_identity": "{}", "grant_types":["client_credentials"] }} }}"""

class ConnectionHandler:
    """
    This is the ConnectionHandler class, responsible for connections
    towards SEPA: HTTP and Websockets.
    """
    def __init__(self, client_id=None, logLevel = 10):
        """Constructor of the ConnectionHandler class"""
        # logger configuration
        self.logger = logging.getLogger("sepaLogger")
        self.logger.setLevel(logLevel)
        self.logger.debug("=== ConnectionHandler::__init__ invoked ===")
        logging.getLogger("urllib3").setLevel(logLevel)
        logging.getLogger("requests").setLevel(logLevel)
        # open subscriptions
        self.websockets = {}
        
        # secure request objects
        self.token = None
        self.client_secret = None
        self.client_id = client_id if client_id else str(uuid4())
        
    def get_client_id(self):
        """
        Getter for the client_id parameter.
        """
        return self.client_id

    def unsecureRequest(self, reqURI, sparql, isQuery):
        """
        Method to issue a SPARQL request over HTTP.
        reqURI is the host destination
        sparql is the SPARQL request
        isQuery is a boolean to identify if the request is a query or an update.
        """
        # debug
        self.logger.debug("=== ConnectionHandler::unsecureRequest invoked ===")
        # perform the request
        headers = {
            "Content-Type":"application/sparql-query" if isQuery else "application/sparql-update", 
            "Accept":"application/sparql-results+json"}
        r = requests.post(reqURI, headers=headers, data=sparql)
        r.connection.close()
        return r.status_code, r.text


    # do HTTPS request
    def secureRequest(self, reqURI, sparql, isQuery, registerURI, tokenURI):
        """
        Method to issue a SPARQL request over HTTPS.
        reqURI is the host destination
        sparql is the SPARQL request
        isQuery is a boolean to identify if the request is a query or an update.
        registerURI is the uri for registration to SEPA
        tokenURI is the JWT
        """
        # debug
        self.logger.debug("=== ConnectionHandler::secureRequest invoked ===")
        
        # if the client is not yet registered, then register!
        if not self.client_secret:
            self.logger.debug("Client secret = {}".format(self.client_secret))
            self.register(registerURI)
        
        # if a token is not present, request it!
        if not self.token:
            self.logger.debug("Token = {}".format(self.token))
            self.requestToken(tokenURI)
                
        # perform the request
        self.logger.debug("Performing a secure SPARQL request")
        headers = {
           "Content-Type":"application/sparql-query" if isQuery else "application/sparql-update", 
           "Accept":"application/json",
           "Authorization": "Bearer " + self.token}
        r = requests.post(reqURI, headers=headers, data=sparql, verify=False)        
        r.connection.close()
            
        # check for errors on token validity
        if r.status_code == 401:
            self.token = None                
            raise TokenExpiredException
        return r.status_code, r.text

    
    ###################################################
    #
    # registration function
    #
    ###################################################

    def register(self, registerURI):
        """
        Method to perform a registration to SEPA.
        registerURI is the url to which ask for registration
        """
        # debug print
        self.logger.debug("=== ConnectionHandler::register invoked ===")
        
        # define headers and payload
        headers = {
            "Content-Type":"application/json",
            "Accept":"application/json"}
        payload = REGISTER_PAYLOAD.format(self.client_id)
        self.logger.debug(payload)

        # perform the request
        self.logger.debug("RegisterURI: {}".format(registerURI))
        r = requests.post(registerURI, headers=headers, data=payload, verify=False)        
        r.connection.close()
        
        if r.status_code == 201:
            # parse the response
            jresponse = json.loads(r.text)["credentials"]

            # encode with base64 client_id and client_secret
            self.client_secret = "Basic {}".format(
                b64encode(bytes(
                    "{}:{}".format(jresponse["client_id"],jresponse["client_secret"]), 
                    "utf-8")).decode("utf-8"))
        else:
            print("{}: {}".format(r.status_code, r.text))
            raise RegistrationFailedException


    ###################################################
    #
    # token request
    #
    ###################################################

    # do request token
    def requestToken(self, tokenURI):
        """
        Method to ask for a JWT to SEPA.
        tokenURI is the url to be contacted.
        """
        # debug print
        self.logger.debug("=== ConnectionHandler::requestToken invoked ===")
        
        # define headers and payload        
        headers = {
            "Content-Type":"application/json", 
            "Accept":"application/json",
            "Authorization": self.client_secret}    

        # perform the request
        r = requests.post(tokenURI, headers=headers, verify=False)        
        r.connection.close()
        if r.status_code == 201:
            self.logger.debug(r.text)
            self.token = json.loads(r.text)["token"]["access_token"]
        else:
            raise TokenRequestFailedException


    ###################################################
    #
    # websocket section
    #
    ###################################################

    def openUnsecureWebsocket(self, 
                              subscribeURI, sparql, alias, handler, 
                              default_graph=None, named_graph=None):
        """
        Opens an unsecure websocket (ws) to run a SEPA subscription.
        subscribeURI is the url of the SEPA dedicated to subscriptions
        sparql is the SPARQL subscription
        alias is the tag of the subscription, for easy recognition
        handler is the function to call when a new notification is received
        default_graph and named_graph allow subscriptions to be more fine grained,
        (look to SEPA documentation for this).
        """
        # debug
        self.logger.debug("=== ConnectionHandler::openUnsecureWebsocket invoked ===")

        # initialization
        handler = handler
        subid = None
        subidEvent = Event()

        # on_message callback
        def on_message(ws, message):
            # Triggered when new messages are received
            nonlocal subid
            nonlocal subidEvent

            self.logger.debug("=== ConnectionHandler::on_message (unsecure) invoked ===")
            self.logger.debug(message)

            # process message
            subid_code, added, removed = parseWSMessage(message)
            
            if ((added is None) and (removed is None)):
                if subid_code is None:
                    # None, None, None case
                    ws.close()
            else:
                # None/Value, value, value case
                if not (subid_code is None):
                    # value, value, value case
                    # save the subscription id and the thread
                    subid = subid_code
                    self.websockets[subid] = ws
                    subidEvent.set()
                handler(added,removed)

        # on_error callback
        def on_error(ws, error):
            self.logger.debug("=== ConnectionHandler::on_error (unsecure) invoked ===")
            self.logger.debug(error)

        # on_close callback
        def on_close(ws):
            self.logger.debug("=== ConnectionHandler::on_close (unsecure) invoked ===")
            # destroy the websocket dictionary
            del self.websockets[subid]

        # on_open callback
        def on_open(ws):           
            self.logger.debug("=== ConnectionHandler::on_open (unsecure) invoked ===")
            # send subscription request
            msg = getSubscriptionRequestMessage(
                sparql, alias, None, default_graph, named_graph)
            ws.send(json.dumps(msg))
            self.logger.debug(msg)

        # configuring the websocket
        ws = WebSocketApp(subscribeURI,
                          on_message = on_message,
                          on_error = on_error,
                          on_close = on_close,
                          on_open = on_open)                                        

        wst = Thread(target=ws.run_forever)
        wst.daemon = True
        wst.start()

        # return
        self.logger.debug("Waiting for subscription ID")
        if not subidEvent.wait(timeout=10):
            raise SubscriptionTimeoutException
        return subid

    
    # do open websocket
    def openSecureWebsocket(self,
                            subscribeURI, sparql, alias, handler, 
                            registerURI, tokenURI,
                            default_graph=None, named_graph=None):
        """
        Opens a secure websocket (wss) to run a SEPA subscription.
        'subscribeURI' is the url of the SEPA dedicated to subscriptions
        'sparql' is the SPARQL subscription
        'alias' is the tag of the subscription, for easy recognition
        'handler' is the function to call when a new notification is received
        'registerURI' is the url to which ask for registration to SEPA
        'tokenURI' is the url to which ask for a JWT
        'default_graph' and 'named_graph' allow subscriptions to be more fine grained,
        (look to SEPA documentation for this).
        """
        # debug
        self.logger.debug("=== ConnectionHandler::openSecureWebsocket invoked ===")
        
        # if the client is not yet registered, then register!
        if not self.client_secret:
            self.register(registerURI)
            
        # if a token is not present, request it!
        if not self.token:
            self.requestToken(tokenURI)

        # initialization
        handler = handler
        subid = None
        subidEvent = Event()

        # on_message callback
        def on_message(ws, message):
            nonlocal subid
            nonlocal subidEvent
            # debug
            self.logger.debug("=== ConnectionHandler::on_message (secure) invoked ===")
            self.logger.debug(message)

            # process message
            subid_code, added, removed = parseWSMessage(message)
            
            if ((added is None) and (removed is None)):
                if subid_code is None:
                    ws.close()
            else:
                if not (subid_code is None):
                    # save the subscription id and the thread
                    subid = subid_code
                    self.websockets[subid] = ws
                    subidEvent.set()
                handler(added,removed)

        # on_error callback
        def on_error(ws, error):
            # debug
            self.logger.debug("=== ConnectionHandler::on_error (unsecure) invoked ===")
            self.logger.debug(error)

        # on_close callback
        def on_close(ws):
            # debug
            self.logger.debug("=== ConnectionHandler::on_close (secure) invoked ===")
            # destroy the websocket dictionary
            del self.websockets[subid]

        # on_open callback
        def on_open(ws):           
            # debug
            self.logger.debug("=== ConnectionHandler::on_open (secure) invoked ===")
            # send subscription request
            msg = getSubscriptionRequestMessage(
                sparql, alias, self.token, default_graph, named_graph)
            ws.send(json.dumps(msg))
            self.logger.debug(msg)


        # configuring the websocket        
        ws = WebSocketApp(subscribeURI,
                          on_message = on_message,
                          on_error = on_error,
                          on_close = on_close,
                          on_open = on_open)                                        

        # starting the websocket thread
        wst = Thread(
            target=ws.run_forever, 
            kwargs=dict(sslopt={"cert_reqs": CERT_NONE}))
        wst.daemon = True
        wst.start()

        # return
        self.logger.debug("Waiting for subscription ID")
        if not subidEvent.wait(timeout=10):
            raise SubscriptionTimeoutException
        return subid
    

    def closeWebsocket(self, subid):
        # debug
        self.logger.debug("=== ConnectionHandler::closeWebSocket invoked ===")
        msg = {}
        msg["unsubscribe"] = {}
        msg["unsubscribe"]["spuid"] = subid
        if self.token:
            msg["unsubscribe"]["authorization"] = "Bearer " + self.token
        self.websockets[subid].send(json.dumps(msg))
        
    def get_subscriptions(self):
        return self.websockets


def parseWSMessage(message):
    subid = None
    logger = logging.getLogger("sepaLogger")
    
    jmessage = json.loads(message)
    if "unsubscribed" in jmessage:
        return None, None, None
    notification = jmessage["notification"]
    if notification["sequence"]==0:
        logger.debug("Subscription Confirmation")
        subid = notification["spuid"]
        logger.debug("SUBID = " + subid)
    
    added = []
    if "addedResults" not in notification.keys():
        logger.warning("No 'addedResults' key in notification")
    elif "results" not in notification["addedResults"].keys():
        logger.warning("No 'results' key in notification['addedResults']")
    elif "bindings" not in notification["addedResults"]["results"].keys():
        logger.warning("No 'bindings' key in notification['addedResults']['results']")
    else:
        added = notification["addedResults"]["results"]["bindings"]
    
    removed = []
    if "removedResults" not in notification.keys():
        logger.warning("No 'removedResults' key in notification")
    elif "results" not in notification["removedResults"].keys():
        logger.warning("No 'results' key in notification['removedResults']")
    elif "bindings" not in notification["removedResults"]["results"].keys():
        logger.warning("No 'bindings' key in notification['removedResults']['results']")
    else:
        removed = notification["removedResults"]["results"]["bindings"]
    return subid, added, removed
        
def getSubscriptionRequestMessage(sparql, alias, token, default_graph, named_graph):
    # composing message
    msg = {}
    msg["subscribe"] = {}
    msg["subscribe"]["sparql"] = sparql
    msg["subscribe"]["alias"] = alias
    if token is not None:
        msg["authorization"] = token
    if default_graph is not None:
        msg["subscribe"]["default-graph-uri"] = default_graph
    if named_graph is not None:
        msg["subscribe"]["default-graph-uri"] = named_graph
    return msg
