const assert = require('assert');
const utils = require('../build/utils');

describe('utils', function() {
  describe('createUri', () => {
    it('test uri function regular', function () {
      assert.equal("http://localhost:8888/path", utils.createURI("http", "localhost", 8888, "/path"))
    });

    it('test uri function regular wrong path', function () {
      assert.throws(() => {
        utils.createURI("http", "localhost", 8888, "hello")
      })
    });

    it('test uri function regular missing path', function () {
      assert.equal("http://localhost:8888", utils.createURI("http", "localhost", 8888))
    });

    it('test uri function regular missing port', function () {
      assert.equal("http://localhost", utils.createURI("http", "localhost"))
    });

    it('test uri function regular missing port with path', function () {
      assert.equal("http://localhost/path", utils.createURI("http", "localhost", undefined, "/path"))
    });


    it('test uri function regular wrong uri', function () {
      assert.throws(() => {
        utils.createURI("http", "loca@l?hos/t", 8888, "hello")
      })
    });
  });

  describe('mergeWithDefaults', () => {
    let defaults = {
      a : "hello",
      b : {
        name : "pippo",
        age : 2
      }
    }

    it('should return the defaults', () => {
      assert.deepEqual(utils.mergeWithDefaults(defaults,{}),defaults,"Merged object is not equal to the defaults")
    });
    
    it('should modify only the a filed ', () => {
      let expected = Object.assign({},defaults)
      expected.a = "welcome"
      assert.deepEqual(utils.mergeWithDefaults(defaults,{ a : "welcome"}),expected,"'a' field is not correct")
    });
    
    it('should modify only the a filed ', () => {
      let expected = Object.assign({},defaults)
      expected.a = "welcome"
      assert.deepEqual(utils.mergeWithDefaults(defaults,{ a : "welcome"}),expected,"'a' field is not correct")
    });
    it("shouldn't remove a field ", () => {
      let arg = {
        b: {
          name: "pippo",
          age: 2
        }
      }
      assert.deepEqual(utils.mergeWithDefaults(defaults,arg),defaults,"'a' field is not correct")
    });
    it("shouldn't remove age field ", () => {
      let arg = {
        a: "hello",
        b: {
          name: "pippo",
         
        }
      }
      assert.deepEqual(utils.mergeWithDefaults(defaults,arg),defaults,"'age' field is not correct")
    });
  });
    
});
