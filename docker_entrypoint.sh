#!/bin/bash
echo "Starting dashboard..."

# Ensure the required environment variable is set
cfg_path=/usr/share/nginx/html/js/config.jsc

echo "Configuration file"
echo $cfg_path

#Load variables
if [ -z "${JSAP_PATH:-}" ]; then
    JSAP_PATH=/usr/share/nginx/html/jsap/chat.jsap
fi

echo "___SEPA_DASHBOARD_INLINE_JSON_CONFIG___ = {" > $cfg_path

echo "\"HOST\":\""$HOST"\"," >> $cfg_path

echo "\"HTTP_PORT\":\""$HTTP_PORT"\"," >> $cfg_path
echo "\"HTTP_PROTOCOL\":\""$HTTP_PROTOCOL"\"," >> $cfg_path
echo "\"WS_PORT\":\""$WS_PORT"\"," >> $cfg_path
echo "\"WS_PROTOCOL\":\""$WS_PROTOCOL"\"," >> $cfg_path

echo "\"UPDATE_PATH\":\""$UPDATE_PATH"\"," >> $cfg_path
echo "\"QUERY_PATH\":\""$QUERY_PATH"\"," >> $cfg_path
echo "\"SUBSCRIBE_PATH\":\""$SUBSCRIBE_PATH"\"," >> $cfg_path

echo "\"JSAP_PATH\":\""$JSAP_PATH"\"," >> $cfg_path


#Load jsap
if [ -z "${JSAP_PATH:-}" ] || [ ! -f "$JSAP_PATH" ]
then
    echo "No default jsap specified"
    echo "\"DEFAULT_JSAP\": null" >> $cfg_path #append empty jsap
else
    echo "\"DEFAULT_JSAP\":" >> $cfg_path
    cat "$JSAP_PATH" >> $cfg_path
    #echo $jsap_value >> $cfg_path
fi

#Close json
echo "}" >> $cfg_path
echo " " >> $cfg_path


cat $cfg_path
# Start Nginx
nginx -g 'daemon off;'
