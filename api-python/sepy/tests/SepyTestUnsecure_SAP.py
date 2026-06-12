#!/usr/bin python3
# -*- coding: utf-8 -*-
#
#  SepyTestUnsecure_SAP.py
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

from pkg_resources import resource_filename

import unittest
from collections import defaultdict

import logging
import yaml
from sepy.SAPObject import SAPObject, generate, YsapTemplate, defaultdict_to_dict
from sepy.SEPA import SEPA
from sepy.tablaze import tablify, check_table_equivalence

EXPECTED_TABLE_test1 = """+----------------------+-----------------+----------------+
|          a           |        b        |       c        |
+----------------------+-----------------+----------------+
| (uri) test:Francesco | (uri) test:dice | (literal) Ciao |
+----------------------+-----------------+----------------+
1 result(s)"""

EXPECTED_TABLE_test2 = """+----------------------+-----------------+
|         nome         |     qualcosa    |
+----------------------+-----------------+
| (uri) test:Francesco |  (literal) Ciao |
|   (uri) test:Fabio   | (literal) Hello |
+----------------------+-----------------+
2 result(s)"""

EXPECTED_TABLE_test3a = """+----------------------+-----------------+
|         nome         |     qualcosa    |
+----------------------+-----------------+
| (uri) test:Francesco |  (literal) Ciao |
|   (uri) test:Fabio   | (literal) Hello |
+----------------------+-----------------+
2 result(s)"""

EXPECTED_TABLE_test3b = """+--------------------+-----------------+
|        nome        |     qualcosa    |
+--------------------+-----------------+
| (uri) test:Adriano | (literal) Salve |
+--------------------+-----------------+
1 result(s)"""


class SepyTestUnsecure_SAP(unittest.TestCase):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        with open(resource_filename(__name__, "testUnsecure.ysap"), "r") as sap_file:
            self.ysap = SAPObject(yaml.load(sap_file))
        self.engine = SEPA(sapObject=self.ysap, logLevel=logging.ERROR)
        self.prefixes = self.ysap.get_namespaces(stringList=True)
        
    def test_0(self):
        self.engine.clear()
            
    def test_1(self):
        self.engine.update("INSERT_GREETING")
        result = self.engine.query_all()

        self.assertEqual(
            tablify(result, prefix_file=self.prefixes, destination=None),
                    EXPECTED_TABLE_test1)
        
    def test_2(self):
        self.assertRaises(
            KeyError, self.engine.update, "INSERT_VARIABLE_GREETING")
        self.engine.update(
            "INSERT_VARIABLE_GREETING",
            forcedBindings={"nome": "test:Fabio", "qualcosa": "Hello"})
        result = self.engine.query("QUERY_GREETINGS")
        self.assertTrue(check_table_equivalence(
            result, EXPECTED_TABLE_test2, self.prefixes))
        
    def test_3(self):
        from threading import Event
        testEvent = Event()
        subid = ""
        notif_counter = 0

        def myHandler(added, removed):
            nonlocal notif_counter
            notif_counter += 1
            addedObject = {}
            addedObject["head"] = {}
            addedObject["head"]["vars"] = list(added[0].keys())
            addedObject["results"] = {}
            addedObject["results"]["bindings"] = added
            if notif_counter == 1:
                self.assertTrue(check_table_equivalence(
                    addedObject, EXPECTED_TABLE_test3a, self.prefixes))
                self.assertEqual(removed, [])
            elif notif_counter == 2:
                self.assertTrue(check_table_equivalence(
                    addedObject, EXPECTED_TABLE_test3b, self.prefixes))
                self.assertEqual(removed, [])
                testEvent.set()
        
        subid = self.engine.subscribe(
            "QUERY_GREETINGS", "test", handler=myHandler)
        self.engine.update(
            "INSERT_VARIABLE_GREETING",
            forcedBindings={"nome": "test:Adriano", "qualcosa": "Salve"})
        self.assertTrue(testEvent.wait())
        self.engine.unsubscribe(subid)
        self.engine.clear()
    
    def test_4(self):
        nested_dict = lambda: defaultdict(nested_dict)
        
        sparql11 = nested_dict()
        sparql11["protocol"] = "http"
        sparql11["port"] = 8000
        sparql11["query"]["path"] = "/query"
        sparql11["query"]["method"] = "POST"
        sparql11["query"]["format"] = "JSON"
        sparql11["update"]["path"] = "/update"
        sparql11["update"]["method"] = "POST"
        sparql11["update"]["format"] = "JSON"
        
        sparql11se = nested_dict()
        sparql11se["protocol"] = "ws"
        sparql11se["availableProtocols"]["ws"]["port"] = 9000
        sparql11se["availableProtocols"]["ws"]["path"] = "/subscribe"
        sparql11se["availableProtocols"]["wss"]["port"] = 9443
        sparql11se["availableProtocols"]["wss"]["path"] = "/secure/subscribe"
        sparql11se = defaultdict_to_dict(sparql11se)
        
        queries = nested_dict()
        queries["QUERY_GREETINGS"]["sparql"] = "select * where {?nome test:dice ?qualcosa}"

        updates = nested_dict()
        updates["INSERT_GREETING"]["sparql"] = "insert data {test:Francesco test:dice 'Ciao'}"
        updates["INSERT_VARIABLE_GREETING"]["sparql"] = "insert data {?nome test:dice ?qualcosa}"
        updates["INSERT_VARIABLE_GREETING"]["forcedBindings"]["nome"]["type"] = "uri"
        updates["INSERT_VARIABLE_GREETING"]["forcedBindings"]["nome"]["value"] = "\"\""
        updates["INSERT_VARIABLE_GREETING"]["forcedBindings"]["qualcosa"]["type"] = "literal"
        updates["INSERT_VARIABLE_GREETING"]["forcedBindings"]["qualcosa"]["value"] = "\"\""
        updates = defaultdict_to_dict(updates)
        
        extended = nested_dict()
        extended["type"] = "basic"
        extended["base"] = 0
        extended["clients"] = 10
        extended["messages"] = 1
        
        namespaces = nested_dict()
        namespaces["schema"] = "http://schema.org"
        namespaces["rdf"] = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        namespaces["test"] = "http://wot.arces.unibo.it/test#"
        
        oauth = nested_dict()
        oauth["enable"] = "false"
        oauth["register"] = "https://localhost:8443/oauth/register"
        oauth["tokenRequest"] = "https://localhost:8443/oauth/token"
        
        sap = generate(YsapTemplate,
                       "localhost",
                       sparql11,
                       sparql11se,
                       queries=queries,
                       updates=updates,
                       namespaces=namespaces,
                       oauth=oauth,
                       extended=extended)
        self.assertEqual(yaml.load(sap), self.ysap.parsed_sap)


if __name__ == '__main__':
    unittest.main(failfast=True)
