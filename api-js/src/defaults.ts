import { AxiosRequestConfig } from 'axios';
import * as https from 'https';
const httpsAgent = https.globalAgent;

export type SEPAConfig = {
	host?: string,
	oauth?: {
		enable?: boolean,
		register?: string,
		tokenRequest?: string
	},
	sparql11protocol?: {
		protocol?: "http" | "https",
		port?: number,
		query?: {
			path?: string,
			method?: "POST" | "GET" | "URL_ENCODED_POST",
			format?: "JSON"
		},
		update?: {
			path?: string,
			method?: "POST" | "GET" | "URL_ENCODED_POST",
			format?: "JSON"
		}
	},
	sparql11seprotocol?: {
		protocol?: "ws" | "wss",
		availableProtocols?: Record<"ws" | "wss", { port?: number, path?: string }>
	},
	options?: AxiosRequestConfig
}; 

const defaults: Required<SEPAConfig> = {
	"host": "localhost",
	"oauth": {
		"enable": false,
		"register": "https://localhost:8443/oauth/register",
		"tokenRequest": "https://localhost:8443/oauth/token"
	},
	"sparql11protocol": {
		"protocol": "http",
		"port": 8000,
		"query": {
			"path": "/query",
			"method": "POST",
			"format": "JSON"
		},
		"update": {
			"path": "/update",
			"method": "POST",
			"format": "JSON"
		}
	},
	"sparql11seprotocol": {
		"protocol": "ws",
		"availableProtocols": {
			"ws": {
				"port": 9000,
				"path": "/subscribe"
			},
			"wss": {
				"port": 9443,
				"path": "/secure/subscribe"
			}
		}
	},
	"options": {
		"httpsAgent": httpsAgent
	}
}
export default Object.freeze(defaults)
