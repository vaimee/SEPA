import defaults from './defaults';
import SEPA from './sepa';
import bench from './querybench';
import jsap from './jsap';

const SECSEPA = require('./secure')


const client = new SEPA(defaults) as (SEPA & { secure: typeof SEPA });
client.secure = SECSEPA;
export = {
  SEPA : SEPA,
  client : client,
  Jsap : jsap,
  bench : bench
}