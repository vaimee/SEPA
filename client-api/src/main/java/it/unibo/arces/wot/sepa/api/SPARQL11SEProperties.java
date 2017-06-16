/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.api;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

/**
 * The Class SPARQL11SEProperties.
 * 
 * { "parameters": { "host": "localhost", "ports": { "http": 8000, "ws": 9000,
 * "https": 8443, "wss": 9443 }, "paths": { "update": "/update", "query":
 * "/query", "subscribe": "/subscribe", "securePath": "/secure", "register":
 * "/oauth/register", "tokenRequest": "/oauth/token" }, "security": {
 * "clientId":
 * "NjA3YjA5NjMtMjIyYy00MTVmLWFhYTEtM2ZhMjIzMTlkM2NhOmM3ZjcxZGRmLWFmNTctNDY0ZC05MjViLWRjNWQ4MTA4ZmEyMw==+MsPJeSo0/Py4oMBai+7XPEdfVEAzU5G3AiiAdT5Kh75xG47x+843PTaTg7FPBrU",
 * "clientSecret":
 * "NjA3YjA5NjMtMjIyYy00MTVmLWFhYTEtM2ZhMjIzMTlkM2NhOmM3ZjcxZGRmLWFmNTctNDY0ZC05MjViLWRjNWQ4MTA4ZmEyMw==+MsPJeSo0/Py4oMBai+7XPEdfVEAzU5G3AiiAdT5Kh75xG47x+843PTaTg7FPBrU72cwZ5VCUzjLOlxbM3hJL7eZpGPVdgzVxwYPZpktzA3ou8FgWd48FuBhE8nSlO6j",
 * "jwt":
 * "NjA3YjA5NjMtMjIyYy00MTVmLWFhYTEtM2ZhMjIzMTlkM2NhOmM3ZjcxZGRmLWFmNTctNDY0ZC05MjViLWRjNWQ4MTA4ZmEyMw==+MsPJeSo0/Py4oMBai+7XPEdfVEAzU5G3AiiAdT5Kh75xG47x+843PTaTg7FPBrU72cwZ5VCUzjLOlxbM3hJL7eZpGPVdgzVxwYPZpktzA3ou8FgWd48FuBhE8nSlO6jxabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfun6peA2iw+Yzgy++JvqYcNK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkWldw9We4PoULmmCLsYMJhsZXJGTP7Ts2MJvFfhzLpNL7RUMQFh1SgaUw0DDIfhMzhOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvsVvET1WCV8SCPO9Eb3ngKzMfrOATYiVTFgkvxW8ILYMjBTTpT/mxs7h+m2x+dUyYHf0dO6ijEwC+4RbAD3G9no7DYxZ6vwE1rgdgjKMO8FDB70btp5wDLC/Ix4ODpjjcEC+5QjFYqLVx1hHcO3OofKlkDTW8SZC2ja+Y0e+DxQBP4KQNicqbb6nHtVquc7evcL7qiBubPC0vCgC0rnG/KnwAuM20poF+9wdxQIRAbEM7FKShSUOUKWiFuwrGwmDo8=03GyA/KHCksRpbTE427Duw==XPrHEX2xHy+5IuXHPHigMw==xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfv8OLnSINurvOL38hvfHCFsK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkWldw9We4PoULmmCLsYMJh2V85wkVIZ+xfygiqcn0BDZ0wcyjgOsLXBmegg4oeHQ/hOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvt7rSSsKqJWuEEPG7XZz8sb5eyV+03bXZ3gtEDElHkpTqgEkE1i97ssq+xQ8FQxQorLmtYAPTbAlAdq0WYKCLqf82XYa0/BQxsSDolYh6LyzhVZFSw38B9M8SZoUy1o2TKgLkwqhbWqSOMqSHadZc1GgudYeL6jnT15YoMDxSgh2ipp9xqfEg14hftHUjnn+eVvbFSrB0/64yhNcy8QEpOTugfcXUOdX/QZFUYYfk0z+pX3VK6kDBvsGt1Gu1OiHUE=s7busLq+ySuYDqnFWs2/lg==XPrHEX2xHy+5IuXHPHigMw==xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfsP07Rrw5G3cPYZKKp1RLZyK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYke9XGiRtdWCLU/9WQpwUqWD1tYwAEOHOn0qZK0z17hqCgkaOp/sLyIGq4J047Pc+HhOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvsUoMF/UIVlrrGwX+ghtGDJtG2OuBC/KY5hLtHN3G7lzmlmSehTw2ec40BqxcLCf2yCmjRSb1xnf+2NtiidKa3qdnk8f2epx/i1vcnG6PLOkSNdJryKVIQSi38X1A/gNtp7hWs97JuVihxDUblVOLWX4TqBDzet8ZvSxxguP+KBGn1CUczRUI28novZLU1KpWTHH64nNLLSLVHuTaQP3mQgisGofYLYhzsW/dnz7OgpbARFXipZ1smUaGbfGSn06sU=",
 * "expires":
 * "NjA3YjA5NjMtMjIyYy00MTVmLWFhYTEtM2ZhMjIzMTlkM2NhOmM3ZjcxZGRmLWFmNTctNDY0ZC05MjViLWRjNWQ4MTA4ZmEyMw==+MsPJeSo0/Py4oMBai+7XPEdfVEAzU5G3AiiAdT5Kh75xG47x+843PTaTg7FPBrU72cwZ5VCUzjLOlxbM3hJL7eZpGPVdgzVxwYPZpktzA3ou8FgWd48FuBhE8nSlO6jxabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfun6peA2iw+Yzgy++JvqYcNK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkWldw9We4PoULmmCLsYMJhsZXJGTP7Ts2MJvFfhzLpNL7RUMQFh1SgaUw0DDIfhMzhOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvsVvET1WCV8SCPO9Eb3ngKzMfrOATYiVTFgkvxW8ILYMjBTTpT/mxs7h+m2x+dUyYHf0dO6ijEwC+4RbAD3G9no7DYxZ6vwE1rgdgjKMO8FDB70btp5wDLC/Ix4ODpjjcEC+5QjFYqLVx1hHcO3OofKlkDTW8SZC2ja+Y0e+DxQBP4KQNicqbb6nHtVquc7evcL7qiBubPC0vCgC0rnG/KnwAuM20poF+9wdxQIRAbEM7FKShSUOUKWiFuwrGwmDo8=03GyA/KHCksRpbTE427Duw==XPrHEX2xHy+5IuXHPHigMw==xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfv8OLnSINurvOL38hvfHCFsK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkWldw9We4PoULmmCLsYMJh2V85wkVIZ+xfygiqcn0BDZ0wcyjgOsLXBmegg4oeHQ/hOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvt7rSSsKqJWuEEPG7XZz8sb5eyV+03bXZ3gtEDElHkpTqgEkE1i97ssq+xQ8FQxQorLmtYAPTbAlAdq0WYKCLqf82XYa0/BQxsSDolYh6LyzhVZFSw38B9M8SZoUy1o2TKgLkwqhbWqSOMqSHadZc1GgudYeL6jnT15YoMDxSgh2ipp9xqfEg14hftHUjnn+eVvbFSrB0/64yhNcy8QEpOTugfcXUOdX/QZFUYYfk0z+pX3VK6kDBvsGt1Gu1OiHUE=s7busLq+ySuYDqnFWs2/lg==XPrHEX2xHy+5IuXHPHigMw==xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfsP07Rrw5G3cPYZKKp1RLZyK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYke9XGiRtdWCLU/9WQpwUqWD1tYwAEOHOn0qZK0z17hqCgkaOp/sLyIGq4J047Pc+HhOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvsUoMF/UIVlrrGwX+ghtGDJtG2OuBC/KY5hLtHN3G7lzmlmSehTw2ec40BqxcLCf2yCmjRSb1xnf+2NtiidKa3qdnk8f2epx/i1vcnG6PLOkSNdJryKVIQSi38X1A/gNtp7hWs97JuVihxDUblVOLWX4TqBDzet8ZvSxxguP+KBGn1CUczRUI28novZLU1KpWTHH64nNLLSLVHuTaQP3mQgisGofYLYhzsW/dnz7OgpbARFXipZ1smUaGbfGSn06sU=C8gpZPX8vmBkyVufxJn4/g==",
 * "type":
 * "NjA3YjA5NjMtMjIyYy00MTVmLWFhYTEtM2ZhMjIzMTlkM2NhOmM3ZjcxZGRmLWFmNTctNDY0ZC05MjViLWRjNWQ4MTA4ZmEyMw==+MsPJeSo0/Py4oMBai+7XPEdfVEAzU5G3AiiAdT5Kh75xG47x+843PTaTg7FPBrU72cwZ5VCUzjLOlxbM3hJL7eZpGPVdgzVxwYPZpktzA3ou8FgWd48FuBhE8nSlO6jxabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfun6peA2iw+Yzgy++JvqYcNK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkWldw9We4PoULmmCLsYMJhsZXJGTP7Ts2MJvFfhzLpNL7RUMQFh1SgaUw0DDIfhMzhOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvsVvET1WCV8SCPO9Eb3ngKzMfrOATYiVTFgkvxW8ILYMjBTTpT/mxs7h+m2x+dUyYHf0dO6ijEwC+4RbAD3G9no7DYxZ6vwE1rgdgjKMO8FDB70btp5wDLC/Ix4ODpjjcEC+5QjFYqLVx1hHcO3OofKlkDTW8SZC2ja+Y0e+DxQBP4KQNicqbb6nHtVquc7evcL7qiBubPC0vCgC0rnG/KnwAuM20poF+9wdxQIRAbEM7FKShSUOUKWiFuwrGwmDo8=03GyA/KHCksRpbTE427Duw==XPrHEX2xHy+5IuXHPHigMw==xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfv8OLnSINurvOL38hvfHCFsK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkWldw9We4PoULmmCLsYMJh2V85wkVIZ+xfygiqcn0BDZ0wcyjgOsLXBmegg4oeHQ/hOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvt7rSSsKqJWuEEPG7XZz8sb5eyV+03bXZ3gtEDElHkpTqgEkE1i97ssq+xQ8FQxQorLmtYAPTbAlAdq0WYKCLqf82XYa0/BQxsSDolYh6LyzhVZFSw38B9M8SZoUy1o2TKgLkwqhbWqSOMqSHadZc1GgudYeL6jnT15YoMDxSgh2ipp9xqfEg14hftHUjnn+eVvbFSrB0/64yhNcy8QEpOTugfcXUOdX/QZFUYYfk0z+pX3VK6kDBvsGt1Gu1OiHUE=s7busLq+ySuYDqnFWs2/lg==XPrHEX2xHy+5IuXHPHigMw==xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfsP07Rrw5G3cPYZKKp1RLZyK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYke9XGiRtdWCLU/9WQpwUqWD1tYwAEOHOn0qZK0z17hqCgkaOp/sLyIGq4J047Pc+HhOux8BQ+F7uOGlugo40Eudeoa44m53VQ6Oagup1VnQGQL99rytyAOMbIxeplhO0sHO9K1qxIma7n12grTP/OX+2z715+eQ0GVaWla2uK1JKQyc0nVfY4fg9zApFTrmvsUoMF/UIVlrrGwX+ghtGDJtG2OuBC/KY5hLtHN3G7lzmlmSehTw2ec40BqxcLCf2yCmjRSb1xnf+2NtiidKa3qdnk8f2epx/i1vcnG6PLOkSNdJryKVIQSi38X1A/gNtp7hWs97JuVihxDUblVOLWX4TqBDzet8ZvSxxguP+KBGn1CUczRUI28novZLU1KpWTHH64nNLLSLVHuTaQP3mQgisGofYLYhzsW/dnz7OgpbARFXipZ1smUaGbfGSn06sU=C8gpZPX8vmBkyVufxJn4/g==XPrHEX2xHy+5IuXHPHigMw=="
 * } } } }
 */
public class SPARQL11SEProperties extends SPARQL11Properties {
	private long expires = 0;
	private String jwt = null;
	private String tokenType = null;
	private String authorization = null;
	private String id = null;
	private String secret = null;

	private int httpsPort;
	private int wsPort;
	private int wssPort;

	private String subscribePath;
	private String registerPath;
	private String tokenRequestPath;
	private String securePath;

	static ByteArrayOutputStream out64 = new ByteArrayOutputStream();

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger("SPARQL11SEProperties");

	/**
	 * The new primitives introduced by the SPARQL 1.1 SE Protocol are:
	 * 
	 * SECUREUPDATE,SECUREQUERY,SUBSCRIBE,SECURESUBSCRIBE,UNSUBSCRIBE,SECUREUNSUBSCRIBE,REGISTER,REQUESTTOKEN
	 * 
	 * 
	 * @author Luca Roffia (luca.roffia@unibo.it)
	 * @version 0.1
	 */
	public enum SPARQL11SEPrimitive {
		/** A secure update primitive */
		SECUREUPDATE,
		/** A subscribe primitive */
		SUBSCRIBE,
		/** A secure subscribe primitive. */
		SECURESUBSCRIBE,
		/** A unsubscribe primitive. */
		UNSUBSCRIBE,
		/** A secure unsubscribe primitive. */
		SECUREUNSUBSCRIBE,
		/** A register primitive. */
		REGISTER,
		/** A request token primitive. */
		REQUESTTOKEN,
		/** A secure query primitive. */
		SECUREQUERY
	};

	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @param secret
	 *            the secret
	 * @throws IOException
	 * @throws NoSuchElementException
	 * @throws FileNotFoundException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws ClassCastException
	 * @throws NullPointerException
	 * @throws InvalidKeyException
	 * @throws NumberFormatException
	 */
	public SPARQL11SEProperties(String propertiesFile, byte[] secret)
			throws FileNotFoundException, NoSuchElementException, IOException, NumberFormatException,
			InvalidKeyException, NullPointerException, ClassCastException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		super(propertiesFile);
		SEPAEncryption.init(secret);
	}

	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @throws IOException
	 * @throws NoSuchElementException
	 * @throws FileNotFoundException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws ClassCastException
	 * @throws NullPointerException
	 * @throws InvalidKeyException
	 */
	public SPARQL11SEProperties(String propertiesFile) throws IllegalArgumentException, FileNotFoundException,
			NoSuchElementException, IOException, InvalidKeyException, NullPointerException, ClassCastException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		this(propertiesFile, null);
		if (propertiesFile == null)
			throw new IllegalArgumentException("Argument is null");
	}

	public String toString() {
		return parameters.toString();
	}

	@Override
	protected void defaults() {
		super.defaults();

		JsonObject ports = parameters.get("ports").getAsJsonObject();
		ports.add("https", new JsonPrimitive(8443));
		ports.add("ws", new JsonPrimitive(9000));
		ports.add("wss", new JsonPrimitive(9443));

		JsonObject paths = parameters.get("paths").getAsJsonObject();
		paths.add("subscribe", new JsonPrimitive("/subscribe"));
		paths.add("register", new JsonPrimitive("/oauth/register"));
		paths.add("tokenRequest", new JsonPrimitive("/oauth/token"));
		paths.add("securePath", new JsonPrimitive("/secure"));
	}

	@Override
	protected void setParameters()
			throws NullPointerException, ClassCastException, IOException, NumberFormatException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		super.setParameters();

		for (Entry<String, JsonElement> elem : parameters.get("ports").getAsJsonObject().entrySet()) {
			if (elem.getKey().equals("https"))
				httpsPort = elem.getValue().getAsInt();
			if (elem.getKey().equals("ws"))
				wsPort = elem.getValue().getAsInt();
			if (elem.getKey().equals("wss"))
				wssPort = elem.getValue().getAsInt();
		}
		for (Entry<String, JsonElement> elem : parameters.get("paths").getAsJsonObject().entrySet()) {
			if (elem.getKey().equals("subscribe"))
				subscribePath = elem.getValue().getAsString();
			if (elem.getKey().equals("register"))
				registerPath = elem.getValue().getAsString();
			if (elem.getKey().equals("tokenRequest"))
				tokenRequestPath = elem.getValue().getAsString();
			if (elem.getKey().equals("securePath"))
				securePath = elem.getValue().getAsString();
		}

		String encryptedValue;
		if (parameters.get("security") != null) {
			if (parameters.get("security").getAsJsonObject().get("expires") != null){
				encryptedValue = parameters.get("security").getAsJsonObject().get("expires").getAsString();
				expires = Long.decode(SEPAEncryption.decrypt(encryptedValue));
			}
			else
				expires = 0;

			if (parameters.get("security").getAsJsonObject().get("jwt") != null) {
				encryptedValue = parameters.get("security").getAsJsonObject().get("jwt").getAsString();
				jwt = SEPAEncryption.decrypt(encryptedValue);
			}
			else
				jwt = null;

			if (parameters.get("security").getAsJsonObject().get("type") != null) {
				encryptedValue = parameters.get("security").getAsJsonObject().get("type").getAsString();
				tokenType = SEPAEncryption.decrypt(encryptedValue);
			}
			else
				tokenType = null;

			if (parameters.get("security").getAsJsonObject().get("client_did") != null
					&& parameters.get("security").getAsJsonObject().get("client_secret") != null) {
				encryptedValue = parameters.get("security").getAsJsonObject().get("client_id").getAsString();
				id = SEPAEncryption.decrypt(encryptedValue);
				
				encryptedValue = parameters.get("security").getAsJsonObject().get("client_secret").getAsString();
				secret = SEPAEncryption.decrypt(encryptedValue);
				
				authorization = Base64.getEncoder().encode((id + ":" + secret).getBytes("UTF-8")).toString();

				// TODO need a "\n", why?
				authorization = authorization.replace("\n", "");

			} else
				authorization = null;
		}
	}

	public String getSecurePath() {
		return securePath;
	}

	public int getWsPort() {
		return wsPort;
	}

	public String getSubscribePath() {
		return subscribePath;
	}

	public int getWssPort() {
		return wssPort;
	}

	public int getHttpsPort() {
		return httpsPort;
	}

	public String getRegisterPath() {
		return registerPath;
	}

	public String getTokenRequestPath() {
		return tokenRequestPath;
	}

	/**
	 * Checks if is token expired.
	 *
	 * @return true, if is token expired
	 */
	public boolean isTokenExpired() {
		return (new Date().getTime() >= expires);
	}

	/**
	 * Gets the expiring seconds.
	 *
	 * @return the expiring seconds
	 */
	public long getExpiringSeconds() {
		long seconds = ((expires - new Date().getTime()) / 1000);
		if (seconds < 0)
			seconds = 0;
		return seconds;
	}

	/**
	 * Gets the access token.
	 *
	 * @return the access token
	 */
	public String getAccessToken() {
		return jwt;
	}

	/**
	 * Gets the token type.
	 *
	 * @return the token type
	 */
	public String getTokenType() {
		return tokenType;
	}

	/**
	 * Gets the basic authorization.
	 *
	 * @return the basic authorization
	 */
	public String getBasicAuthorization() {
		return authorization;
	}

	/**
	 * Sets the credentials.
	 *
	 * @param id
	 *            the username
	 * @param secret
	 *            the password
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	public void setCredentials(String id, String secret) throws IOException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		logger.debug("Set credentials Id: " + id + " Secret:" + secret);

		this.id = id;
		this.secret = secret;

		authorization = new String(Base64.getEncoder().encode((id + ":" + secret).getBytes("UTF-8")));

		// TODO need a "\n", why?
		//authorization = authorization.replace("\n", "");

		// Save on file the encrypted version
		if (parameters.get("security") == null) {
			JsonObject credentials = new JsonObject();
			credentials.add("client_id", new JsonPrimitive(SEPAEncryption.encrypt(id)));
			credentials.add("client_secret", new JsonPrimitive(SEPAEncryption.encrypt(secret)));
			parameters.add("security", credentials);
		} else {
			parameters.get("security").getAsJsonObject().add("client_id", new JsonPrimitive(SEPAEncryption.encrypt(id)));
			parameters.get("security").getAsJsonObject().add("client_secret",
					new JsonPrimitive(SEPAEncryption.encrypt(secret)));
		}

		storeProperties(propertiesFile);
	}

	/**
	 * Sets the JWT.
	 *
	 * @param jwt
	 *            the JSON Web Token
	 * @param expires
	 *            the date when the token will expire
	 * @param type
	 *            the token type (e.g., bearer)
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	public void setJWT(String jwt, Date expires, String type) throws IOException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {

		this.jwt = jwt;
		this.expires = expires.getTime();
		this.tokenType = type;

		// Save on file the encrypted version
		if (parameters.get("security") == null) {
			JsonObject credentials = new JsonObject();
			credentials.add("jwt", new JsonPrimitive(SEPAEncryption.encrypt(jwt)));
			credentials.add("expires",
					new JsonPrimitive(SEPAEncryption.encrypt(String.format("%d", expires.getTime()))));
			credentials.add("type", new JsonPrimitive(SEPAEncryption.encrypt(type)));
			parameters.add("security", credentials);
		} else {
			parameters.get("security").getAsJsonObject().add("jwt", new JsonPrimitive(SEPAEncryption.encrypt(jwt)));
			parameters.get("security").getAsJsonObject().add("expires",
					new JsonPrimitive(SEPAEncryption.encrypt(String.format("%d", expires.getTime()))));
			parameters.get("security").getAsJsonObject().add("type", new JsonPrimitive(SEPAEncryption.encrypt(type)));
		}

		storeProperties(propertiesFile);
	}

	/**
	 * The Class SEPAEncryption.
	 */
	private static class SEPAEncryption {

		/** The Constant ALGO. */
		// AES 128 bits (16 bytes)
		private static final String ALGO = "AES";

		/** The key value. */
		private static byte[] keyValue = new byte[] { '0', '1', 'R', 'a', 'v', 'a', 'm', 'i', '!', 'I', 'e', '2', '3',
				'7', 'A', 'N' };

		/** The key. */
		private static Key key = new SecretKeySpec(keyValue, ALGO);

		/**
		 * Inits the.
		 *
		 * @param secret
		 *            the secret
		 */
		private static void init(byte[] secret) {
			if (secret != null && secret.length == 16)
				keyValue = secret;
			key = new SecretKeySpec(keyValue, ALGO);
		}

		/**
		 * Encrypt.
		 *
		 * @param Data
		 *            the data
		 * @return the string
		 * @throws IOException
		 * @throws NoSuchPaddingException
		 * @throws NoSuchAlgorithmException
		 * @throws InvalidKeyException
		 * @throws BadPaddingException
		 * @throws IllegalBlockSizeException
		 */
		public static String encrypt(String Data) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException,
				InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.ENCRYPT_MODE, key);
			return new String(Base64.getEncoder().encode(c.doFinal(Data.getBytes("UTF-8"))));
		}

		/**
		 * Decrypt.
		 *
		 * @param encryptedData
		 *            the encrypted data
		 * @return the string
		 * @throws NoSuchPaddingException
		 * @throws NoSuchAlgorithmException
		 * @throws InvalidKeyException
		 * @throws BadPaddingException
		 * @throws IllegalBlockSizeException
		 */
		public static String decrypt(String encryptedData) throws NoSuchAlgorithmException, NoSuchPaddingException,
				InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.DECRYPT_MODE, key);
			return new String(c.doFinal(Base64.getDecoder().decode(encryptedData)));
		}
	}
}
