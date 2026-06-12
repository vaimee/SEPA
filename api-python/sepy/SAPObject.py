#!/usr/bin/python3
# -*- coding: utf-8 -*-
#
#  SAPObject.py
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

from urllib.parse import urlparse, urlunparse
from jinja2 import Environment, FileSystemLoader, BaseLoader
from os.path import split, abspath, isfile
from pkg_resources import resource_filename
from collections import defaultdict
from io import TextIOBase

import logging

YsapTemplate = resource_filename(__name__, "ysap_template.sap")


class SAPObject:
    """
    The SAPObject class unifies the formats of SAP files. The only need
    is to parse the file with the suitable libraries, and give to the 
    constructor a python3 dictionary built as specified in SEPADocs.
    """
    def __init__(self, parsed_sap_dict, log=logging.DEBUG):
        """
        SAPObject Constructor. 
        parsed_sap_dict must be a dictionary.
        """
        self.parsed_sap = parsed_sap_dict
        self.logger = logging.getLogger("sapLogger")
        logging.basicConfig(format='%(levelname)s:%(message)s', level=log)

    def explore(self, path):
        """
        Generic SAP dictionary explorer.
        path is a list of indices to be followed according to SEPADocs.
        """
        item = self.parsed_sap
        try:
            for tag in path:
                item = item[tag]
        except KeyError as ke:
            self.logger.error(ke)
            return None
        return item

    def sparql11protocol(self, path=[]):
        """
        Explorer of the sparql11protocol subobject.
        Use 'path' to get a specific value according to SEPADocs.
        """
        return self.explore(["sparql11protocol"]+path)

    def sparql11seprotocol(self, path=[]):
        """
        Explorer of the sparql11seprotocol subobject.
        Use 'path' to get a specific value according to SEPADocs.
        """
        return self.explore(["sparql11seprotocol"]+path)

    @property
    def host(self):
        """
        Getter for the 'host' SAP item
        """
        return self.parsed_sap["host"]

    @property
    def graphs(self):
        """
        Getter for the 'graphs" SAP subobject
        """
        try:
            g = self.parsed_sap["graphs"]
        except KeyError:
            self.logger.debug("'graph' entry not found in sap file")
            g = {}
        return g

    @property
    def registration_url(self):
        """
        Getter for the 'oauth' registration url
        """
        return self.explore(["oauth", "register"])

    @property
    def tokenRequest_url(self):
        """
        Getter for the 'oauth' tokenRequest_url
        """
        return self.explore(["oauth", "tokenRequest"])

    @property
    def query_url(self):
        """
        According to SAP data, this method builds up the query url to
        which send query requests.
        """
        return urlunparse([
            self.sparql11protocol(path=["protocol"]),
            "{}:{}".format(self.host, self.sparql11protocol(path=["port"])),
            self.sparql11protocol(["query", "path"]),
            "", "", ""])

    @property
    def update_url(self):
        """
        According to SAP data, this method builds up the update url to
        which send update requests
        """
        return urlunparse([
            self.sparql11protocol(path=["protocol"]),
            "{}:{}".format(self.host, self.sparql11protocol(path=["port"])),
            self.sparql11protocol(["update", "path"]),
            "", "", ""])

    @property
    def subscribe_url(self):
        """
        According to SAP data, this method builds up the subscribe url 
        to which send subscription and unsubscription requests
        """
        use_protocol = self.sparql11seprotocol(["protocol"])
        return urlunparse([
            use_protocol,
            "{}:{}".format(self.host, self.sparql11seprotocol(["availableProtocols", use_protocol, "port"])),
            self.sparql11seprotocol(["availableProtocols", use_protocol, "path"]),
            "", "", ""])

    @property
    def updates(self):
        """
        Gets the SAP object containing updates. You can access to specific
        updates by using the dict indexes.
        """
        return self.explore(["updates"])

    @property
    def queries(self):
        """
        Gets the SAP object containing queries. You can access to specific
        queries by using the dict indexes.
        """
        return self.explore(["queries"])

    def getSparql(self,
                  sparqlSet,
                  identifier, forcedBindings={}, bindingCheck=True):
        """
        Get a sparql from the sap, and performs forced bindings check and
        substitution.
        'sparqlSet' is 'updates' or 'queries',
        'identifier' is the SAP sparql identification tag
        'forcedBindings' is a dictionary in which you put the bindings to
        be substituted when performing the query.
        'bindingCheck' is a flag, usually set to True, that checks that 
        ALL required bindings have been specified in forcedBindings,
        otherwise raising exception. The check will be skipped, if set to
        False.
        """
        if "forcedBindings" in sparqlSet[identifier]:
            bindings = sparqlSet[identifier]["forcedBindings"]
            if bindingCheck:
                checkBindings(forcedBindings, bindings)
            for b in (set(bindings.keys()) & set(forcedBindings.keys())):
                bindings[b]["value"] = forcedBindings[b]
        else:
            bindings = {}
        return sparqlBuilder(
            sparqlSet[identifier]["sparql"],
            bindings,
            namespaces=self.get_namespaces(stringList=True))

    def getUpdate(self, identifier, forcedBindings={}):
        """
        See getSparql, with 'sparqlSet' as 'updates'
        """
        return self.getSparql(self.updates, identifier, forcedBindings)

    def getQuery(self, identifier, forcedBindings={}):
        """
        See getSparql, with 'sparqlSet' as 'queries'
        """
        return self.getSparql(self.queries, identifier, forcedBindings)

    def get_namespaces(self, stringList=False):
        """
        From SAP dictionary, this is a getter that retrieves namespaces.
        If 'stringList' is set to False, the method returns a dictionary
        "tag": "value". If it is set to true, it returns a List of
        strings formatted as required by sparql language:
        "PREFIX 'tag': <'value'>"
        """
        namespaces = self.explore(["namespaces"])
        if stringList:
            return [
                "PREFIX {}: <{}>".format(ns, uri)
                for ns, uri in namespaces.items()]
        else:
            return namespaces

    def update_namespaces(self, ns_id, ns_uri):
        self.get_namespaces()[ns_id] = ns_uri


def checkBindings(current, expected):
    """
    This method checks that you give the appropriate forced bindings
    to the sepa instance. In an ysap, the required bindings are the
    ones that do not have a default value. Which means the ones that
    have their value == "".
    """
    set_current = set(current.keys())
    set_expected = set(expected.keys())
    # let's take the bindings that are expected from the ysap but not
    # available among those currently given. If one of the expected
    # has value "" (i.e. it is required) an exception is thrown.
    set_difference = set_expected - set_current
    if len(set_difference) != 0:
        for key in set_difference:
            if expected[key]["value"] == "":
                raise KeyError(key+" is a required forcedbinding")
    return True


def uriFormat(uri):
    """
    Checks if 'uri' is being formatted as rdf:type, or
    <http://www.google.it>
    """
    parseBN_URI = urlparse(uri)
    if parseBN_URI.scheme == "" or parseBN_URI.netloc == "":
        # prefixed uri, like rdf:type
        return uri
    else:
        return "<"+uri+">"


def expand_prefixed_uri(uri, namespaces):
    """
    This function takes an 'uri' as rdf:type and transforms it into
    the long format <http://...>, looking in the 'namespaces' given
    """
    splitted_uri = uri.split(":", 1)
    return uriFormat(namespaces[splitted_uri[0]]+splitted_uri[1])


def sparqlBuilder(unbound_sparql, bindings, namespaces=[]):
    """
    Forced bindings substitution into unbounded SPARQL
    """
    sparql = " ".join(namespaces) + " " + unbound_sparql
    for b in bindings.keys():
        bValue = bindings[b]["value"]
        if bValue is not None:
            if (bindings[b]["type"] == "literal") and (bValue != "UNDEF"):
                sparql = sparql.replace("?"+b, "'"+bValue+"'")
            else:
                sparql = sparql.replace("?"+b, uriFormat(bValue))
    return sparql


def generate(sap_template,
             host,
             sparql11,
             sparql11se,
             queries=None,
             updates=None,
             namespaces=None,
             graphs=None,
             extended=None,
             oauth=None,
             destination_file=None):
    """
    Generates an ysap file, and returns it as string.
    If destination_file is given, the file is created and written at
    the corresponding path. The same is returned as string.
    """
    if sap_template is None:
        raise ValueError("'sap_template' cannot be None")
    if host is None:
        raise ValueError("'host' cannot be None")
    if sparql11 is None:
        raise ValueError("'sparql11' cannot be None")
    if sparql11se is None:
        raise ValueError("'sparql11se' cannot be None")
    if ((queries is None or queries == {}) and (updates is None or updates == {})):
        raise ValueError("'queries' and 'updates' cannot be both None or empty")
    logger = logging.getLogger("sapLogger")
    logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)

    if isfile(sap_template):
        logger.debug("'sap_template' is a path to existing file")
        sap_dir, sap_file = split(abspath(sap_template))
        j2_env = Environment(
            loader=FileSystemLoader(sap_dir),
            trim_blocks=True, lstrip_blocks=True)
        template = j2_env.get_template(sap_file)
    else:
        logger.warning("'sap_template' is interpreted as string")
        j2_env = Environment(
            loader=BaseLoader(),
            trim_blocks=True, lstrip_blocks=True)
        template = j2_env.from_string(sap_template)
    
    render_data = {}
    render_data["host_ip_address"] = host
    
    if ["protocol", "port", "query", "update"] != list(sparql11.keys()):
        raise KeyError("Missing key in sparql11 parameter")
    if ["path", "method", "format"] != list(sparql11["query"].keys()):
        raise KeyError("Missing key in sparql11::query parameter")
    if ["path", "method", "format"] != list(sparql11["update"].keys()):
        raise KeyError("Missing key in sparql11::update parameter")
    render_data["sparql11"] = sparql11
    
    logger.warning("No check is made on sparql11se content, except 'protocol'!")
    if "protocol" not in sparql11se.keys():
        raise KeyError("Missing 'protocol' key in sparql11se parameter")
    render_data["sparql11se"] = sparql11se
    
    logger.warning("No check is made on queries and updates, except 'sparql'!")
    if (queries is not None) and (queries != {}):
        for key in queries.keys():
            if "sparql" not in queries[key].keys():
                raise KeyError(
                    "Missing 'sparql' key in queries::{}".format(key))
        render_data["queries"] = queries
    if (updates is not None) and (updates != {}):
        for key in updates.keys():
            if "sparql" not in updates[key].keys():
                raise KeyError(
                    "Missing 'sparql' key in updates::{}".format(key))
        render_data["updates"] = updates
    if oauth is not None:
        logger.warning("No check is made on oauth content!")
        render_data["oauth"] = oauth
    if namespaces is not None:
        logger.warning("No check is made on namespaces content!")
        render_data["namespaces"] = namespaces
    if graphs is not None:
        logger.warning("No check is made on graphs content!")
        render_data["graphs"] = graphs
    if extended is not None:
        logger.warning("No check is made on extended content!")
        render_data["extended"] = extended
    
    sapFileString = template.render(render_data)
    if destination_file is not None:
        try:
            if isinstance(destination_file, TextIOBase):
                print(sapFileString, file=destination_file)
            else:
                with open(destination_file, "w") as csap:
                    print(sapFileString, file=csap)
        except Exception as e:
            logger.error("Unable to export sapFile: {}".format(e))
    return sapFileString


def defaultdict_to_dict(d):
    """
    Utility that transforms python defaultdicts to dicts
    """
    if isinstance(d, defaultdict):
        d = {k: defaultdict_to_dict(v) for k, v in d.items()}
    return d
