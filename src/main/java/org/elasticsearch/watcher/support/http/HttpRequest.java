/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.watcher.support.http;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.support.WatcherDateTimeUtils;
import org.elasticsearch.watcher.support.WatcherUtils;
import org.elasticsearch.watcher.support.http.auth.HttpAuth;
import org.elasticsearch.watcher.support.http.auth.HttpAuthRegistry;

import java.io.IOException;
import java.util.Map;

public class HttpRequest implements ToXContent {

    final String host;
    final int port;
    final Scheme scheme;
    final HttpMethod method;
    final @Nullable String path;
    final ImmutableMap<String, String> params;
    final ImmutableMap<String, String> headers;
    final @Nullable HttpAuth auth;
    final @Nullable String body;
    final @Nullable TimeValue connectionTimeout;
    final @Nullable TimeValue readTimeout;

    public HttpRequest(String host, int port, @Nullable Scheme scheme, @Nullable HttpMethod method, @Nullable String path,
                       @Nullable ImmutableMap<String, String> params, @Nullable ImmutableMap<String, String> headers,
                       @Nullable HttpAuth auth, @Nullable String body, @Nullable TimeValue connectionTimeout, @Nullable TimeValue readTimeout) {
        this.host = host;
        this.port = port;
        this.scheme = scheme != null ? scheme : Scheme.HTTP;
        this.method = method != null ? method : HttpMethod.GET;
        this.path = path;
        this.params = params != null ? params : ImmutableMap.<String, String>of();
        this.headers = headers != null ? headers : ImmutableMap.<String, String>of();
        this.auth = auth;
        this.body = body;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public Scheme scheme() {
        return scheme;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public HttpMethod method() {
        return method;
    }

    public String path() {
        return path;
    }

    public Map<String, String> params() {
        return params;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public HttpAuth auth() {
        return auth;
    }

    public boolean hasBody() {
        return body != null;
    }

    public String body() {
        return body;
    }

    public TimeValue connectionTimeout() {
        return connectionTimeout;
    }

    public TimeValue readTimeout() {
        return readTimeout;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field(Field.HOST.getPreferredName(), host);
        builder.field(Field.PORT.getPreferredName(), port);
        builder.field(Field.SCHEME.getPreferredName(), scheme, params);
        builder.field(Field.METHOD.getPreferredName(), method, params);
        if (path != null) {
            builder.field(Field.PATH.getPreferredName(), path);
        }
        if (!this.params.isEmpty()) {
            builder.field(Field.PARAMS.getPreferredName(), this.params);
        }
        if (!headers.isEmpty()) {
            builder.field(Field.HEADERS.getPreferredName(), headers);
        }
        if (auth != null) {
            builder.field(Field.AUTH.getPreferredName(), auth, params);
        }
        if (body != null) {
            builder.field(Field.BODY.getPreferredName(), body);
        }
        if (connectionTimeout != null) {
            builder.field(Field.CONNECTION_TIMEOUT.getPreferredName(), connectionTimeout);
        }
        if (readTimeout != null) {
            builder.field(Field.READ_TIMEOUT.getPreferredName(), readTimeout);
        }
        return builder.endObject();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpRequest that = (HttpRequest) o;

        if (port != that.port) return false;
        if (!host.equals(that.host)) return false;
        if (scheme != that.scheme) return false;
        if (method != that.method) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (!params.equals(that.params)) return false;
        if (!headers.equals(that.headers)) return false;
        if (auth != null ? !auth.equals(that.auth) : that.auth != null) return false;
        if (connectionTimeout != null ? !connectionTimeout.equals(that.connectionTimeout) : that.connectionTimeout != null) return false;
        if (readTimeout != null ? !readTimeout.equals(that.readTimeout) : that.readTimeout != null) return false;
        return !(body != null ? !body.equals(that.body) : that.body != null);

    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        result = 31 * result + scheme.hashCode();
        result = 31 * result + method.hashCode();
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + params.hashCode();
        result = 31 * result + headers.hashCode();
        result = 31 * result + (auth != null ? auth.hashCode() : 0);
        result = 31 * result + (connectionTimeout != null ? connectionTimeout.hashCode() : 0);
        result = 31 * result + (readTimeout != null ? readTimeout.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "auth=[" + (auth != null ? "******" : null) +
                "], body=[" + body + '\'' +
                "], path=[" + path + '\'' +
                "], method=[" + method +
                "], port=[" + port +
                "], host=[" + host + '\'' +
                "], connection_timeout=[" + connectionTimeout + '\'' +
                "], read_timeout=[" + readTimeout + '\'' +
                "]}";
    }

    public static Builder builder(String host, int port) {
        return new Builder(host, port);
    }

    public static class Parser {

        private final HttpAuthRegistry httpAuthRegistry;

        @Inject
        public Parser(HttpAuthRegistry httpAuthRegistry) {
            this.httpAuthRegistry = httpAuthRegistry;
        }

        public HttpRequest parse(XContentParser parser) throws IOException {
            Builder builder = new Builder();
            XContentParser.Token token;
            String currentFieldName = null;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.AUTH)) {
                    builder.auth(httpAuthRegistry.parse(parser));
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.CONNECTION_TIMEOUT)) {
                    try {
                        builder.connectionTimeout(WatcherDateTimeUtils.parseTimeValue(parser, Field.CONNECTION_TIMEOUT.toString()));
                    } catch (ElasticsearchParseException pe) {
                        throw new ElasticsearchParseException("could not parse http request. invalid time value for [{}] field", pe, currentFieldName);
                    }
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.READ_TIMEOUT)) {
                    try {
                        builder.readTimeout(WatcherDateTimeUtils.parseTimeValue(parser, Field.READ_TIMEOUT.toString()));
                    } catch (ElasticsearchParseException pe) {
                        throw new ElasticsearchParseException("could not parse http request. invalid time value for [{}] field", pe, currentFieldName);
                    }
                } else if (token == XContentParser.Token.START_OBJECT) {
                    if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.HEADERS)) {
                        builder.setHeaders((Map) WatcherUtils.flattenModel(parser.map()));
                    } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.PARAMS)) {
                        builder.setParams((Map) WatcherUtils.flattenModel(parser.map()));
                    }  else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.BODY)) {
                        builder.body(parser.text());
                    } else {
                        throw new ElasticsearchParseException("could not parse http request. unexpected object field [{}]", currentFieldName);
                    }
                } else if (token == XContentParser.Token.VALUE_STRING) {
                    if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.SCHEME)) {
                        builder.scheme(Scheme.parse(parser.text()));
                    } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.METHOD)) {
                        builder.method(HttpMethod.parse(parser.text()));
                    } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.HOST)) {
                        builder.host = parser.text();
                    } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.PATH)) {
                        builder.path(parser.text());
                    } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.BODY)) {
                        builder.body(parser.text());
                    } else {
                        throw new ElasticsearchParseException("could not parse http request. unexpected string field [{}]", currentFieldName);
                    }
                } else if (token == XContentParser.Token.VALUE_NUMBER) {
                    if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.PORT)) {
                        builder.port = parser.intValue();
                    } else {
                        throw new ElasticsearchParseException("could not parse http request. unexpected numeric field [{}]", currentFieldName);
                    }
                } else {
                    throw new ElasticsearchParseException("could not parse http request. unexpected token [{}]", token);
                }
            }

            if (builder.host == null) {
                throw new ElasticsearchParseException("could not parse http request. missing required [{}] field", Field.HOST.getPreferredName());
            }

            if (builder.port < 0) {
                throw new ElasticsearchParseException("could not parse http request. missing required [{}] field", Field.PORT.getPreferredName());
            }

            return builder.build();
        }
    }

    public static class Builder {

        private String host;
        private int port = -1;
        private Scheme scheme;
        private HttpMethod method;
        private String path;
        private ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
        private ImmutableMap.Builder<String, String> headers = ImmutableMap.builder();
        private HttpAuth auth;
        private String body;
        private TimeValue connectionTimeout;
        private TimeValue readTimeout;

        private Builder(String host, int port) {
            this.host = host;
            this.port = port;
        }

        private Builder() {
        }

        public Builder scheme(Scheme scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder setParams(Map<String, String> params) {
            this.params.putAll(params);
            return this;
        }

        public Builder setParam(String key, String value) {
            this.params.put(key, value);
            return this;
        }

        public Builder setHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder setHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder auth(HttpAuth auth) {
            this.auth = auth;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder connectionTimeout(TimeValue timeout) {
            this.connectionTimeout = timeout;
            return this;
        }

        public Builder readTimeout(TimeValue timeout) {
            this.readTimeout = timeout;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(host, port, scheme, method, path, params.build(), headers.build(), auth, body, connectionTimeout, readTimeout);
        }
    }

    public interface Field {
        ParseField SCHEME = new ParseField("scheme");
        ParseField HOST = new ParseField("host");
        ParseField PORT = new ParseField("port");
        ParseField METHOD = new ParseField("method");
        ParseField PATH = new ParseField("path");
        ParseField PARAMS = new ParseField("params");
        ParseField HEADERS = new ParseField("headers");
        ParseField AUTH = new ParseField("auth");
        ParseField BODY = new ParseField("body");
        ParseField CONNECTION_TIMEOUT = new ParseField("connection_timeout");
        ParseField READ_TIMEOUT = new ParseField("read_timeout");
    }
}
