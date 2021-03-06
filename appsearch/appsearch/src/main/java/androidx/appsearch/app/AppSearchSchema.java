/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appsearch.app;

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appsearch.exceptions.IllegalSchemaException;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Set;

/**
 * The AppSearch Schema for a particular type of document.
 *
 * <p>For example, an e-mail message or a music recording could be a schema type.
 *
 * <p>The schema consists of type information, properties, and config (like tokenization type).
 */
public final class AppSearchSchema {
    static final String SCHEMA_TYPE_FIELD = "schemaType";
    static final String PROPERTIES_FIELD = "properties";

    private final Bundle mBundle;

    AppSearchSchema(@NonNull Bundle bundle) {
        Preconditions.checkNotNull(bundle);
        mBundle = bundle;
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     * @hide
     */
    @NonNull
    Bundle getBundle() {
        return mBundle;
    }

    @Override
    public String toString() {
        return mBundle.toString();
    }

    /** Builder for {@link AppSearchSchema objects}. */
    public static final class Builder {
        private final String mTypeName;
        private final ArrayList<Bundle> mProperties = new ArrayList<>();
        private final Set<String> mPropertyNames = new ArraySet<>();
        private boolean mBuilt = false;

        /** Creates a new {@link AppSearchSchema.Builder}. */
        public Builder(@NonNull String typeName) {
            Preconditions.checkNotNull(typeName);
            mTypeName = typeName;
        }

        /** Adds a property to the given type. */
        @NonNull
        public AppSearchSchema.Builder addProperty(@NonNull PropertyConfig propertyConfig) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(propertyConfig);
            if (!mPropertyNames.add(propertyConfig.mName)) {
                throw new IllegalSchemaException(
                        "Property defined more than once: " + propertyConfig.mName);
            }
            mProperties.add(propertyConfig.mBundle);
            return this;
        }

        /**
         * Constructs a new {@link AppSearchSchema} from the contents of this builder.
         *
         * <p>After calling this method, the builder must no longer be used.
         */
        @NonNull
        public AppSearchSchema build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Bundle bundle = new Bundle();
            bundle.putString(AppSearchSchema.SCHEMA_TYPE_FIELD, mTypeName);
            bundle.putParcelableArrayList(AppSearchSchema.PROPERTIES_FIELD, mProperties);
            mBuilt = true;
            return new AppSearchSchema(bundle);
        }
    }

    /**
     * Configuration for a single property (field) of a document type.
     *
     * <p>For example, an {@code EmailMessage} would be a type and the {@code subject} would be
     * a property.
     */
    public static final class PropertyConfig {
        static final String NAME_FIELD = "name";
        static final String DATA_TYPE_FIELD = "dataType";
        static final String SCHEMA_TYPE_FIELD = "schemaType";
        static final String CARDINALITY_FIELD = "cardinality";
        static final String INDEXING_TYPE_FIELD = "indexingType";
        static final String TOKENIZER_TYPE_FIELD = "tokenizerType";

        /**
         * Physical data-types of the contents of the property.
         * @hide
         */
        // NOTE: The integer values of these constants must match the proto enum constants in
        // com.google.android.icing.proto.PropertyConfigProto.DataType.Code.
        @IntDef(value = {
                DATA_TYPE_STRING,
                DATA_TYPE_INT64,
                DATA_TYPE_DOUBLE,
                DATA_TYPE_BOOLEAN,
                DATA_TYPE_BYTES,
                DATA_TYPE_DOCUMENT,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DataType {}

        public static final int DATA_TYPE_STRING = 1;
        public static final int DATA_TYPE_INT64 = 2;
        public static final int DATA_TYPE_DOUBLE = 3;
        public static final int DATA_TYPE_BOOLEAN = 4;

        /** Unstructured BLOB. */
        public static final int DATA_TYPE_BYTES = 5;

        /**
         * Indicates that the property itself is an Document, making it part a hierarchical
         * Document schema. Any property using this DataType MUST have a valid
         * {@code schemaType}.
         */
        public static final int DATA_TYPE_DOCUMENT = 6;

        /**
         * The cardinality of the property (whether it is required, optional or repeated).
         * @hide
         */
        // NOTE: The integer values of these constants must match the proto enum constants in
        // com.google.android.icing.proto.PropertyConfigProto.Cardinality.Code.
        @IntDef(value = {
                CARDINALITY_REPEATED,
                CARDINALITY_OPTIONAL,
                CARDINALITY_REQUIRED,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Cardinality {}

        /** Any number of items (including zero) [0...*]. */
        public static final int CARDINALITY_REPEATED = 1;

        /** Zero or one value [0,1]. */
        public static final int CARDINALITY_OPTIONAL = 2;

        /** Exactly one value [1]. */
        public static final int CARDINALITY_REQUIRED = 3;

        /**
         * Encapsulates the configurations on how AppSearch should query/index these terms.
         * @hide
         */
        @IntDef(value = {
                INDEXING_TYPE_NONE,
                INDEXING_TYPE_EXACT_TERMS,
                INDEXING_TYPE_PREFIXES,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface IndexingType {}

        /**
         * Content in this property will not be tokenized or indexed.
         *
         * <p>Useful if the data type is not made up of terms (e.g.
         * {@link PropertyConfig#DATA_TYPE_DOCUMENT} or {@link PropertyConfig#DATA_TYPE_BYTES}
         * type). All the properties inside the nested property won't be indexed regardless of the
         * value of {@code indexingType} for the nested properties.
         */
        public static final int INDEXING_TYPE_NONE = 0;

        /**
         * Content in this property should only be returned for queries matching the exact tokens
         * appearing in this property.
         *
         * <p>Ex. A property with "fool" should NOT match a query for "foo".
         */
        public static final int INDEXING_TYPE_EXACT_TERMS = 1;

        /**
         * Content in this property should be returned for queries that are either exact matches or
         * query matches of the tokens appearing in this property.
         *
         * <p>Ex. A property with "fool" <b>should</b> match a query for "foo".
         */
        public static final int INDEXING_TYPE_PREFIXES = 2;

        /**
         * Configures how tokens should be extracted from this property.
         * @hide
         */
        // NOTE: The integer values of these constants must match the proto enum constants in
        // com.google.android.icing.proto.IndexingConfig.TokenizerType.Code.
        @IntDef(value = {
                TOKENIZER_TYPE_NONE,
                TOKENIZER_TYPE_PLAIN,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface TokenizerType {}

        /**
         * It is only valid for tokenizer_type to be 'NONE' if the data type is
         * {@link PropertyConfig#DATA_TYPE_DOCUMENT}.
         */
        public static final int TOKENIZER_TYPE_NONE = 0;

        /** Tokenization for plain text. */
        public static final int TOKENIZER_TYPE_PLAIN = 1;

        final String mName;
        final Bundle mBundle;

        PropertyConfig(@NonNull String name, @NonNull Bundle bundle) {
            mName = Preconditions.checkNotNull(name);
            mBundle = Preconditions.checkNotNull(bundle);
        }

        @Override
        public String toString() {
            return mBundle.toString();
        }

        /**
         * Builder for {@link PropertyConfig}.
         *
         * <p>The following properties must be set, or {@link PropertyConfig} construction will
         * fail:
         * <ul>
         *     <li>dataType
         *     <li>cardinality
         * </ul>
         *
         * <p>In addition, if {@code schemaType} is {@link #DATA_TYPE_DOCUMENT}, {@code schemaType}
         * is also required.
         */
        public static final class Builder {
            private final String mName;
            private final Bundle mBundle = new Bundle();
            private boolean mBuilt = false;

            /** Creates a new {@link PropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mName = Preconditions.checkNotNull(propertyName);
                mBundle.putString(NAME_FIELD, propertyName);
            }

            /**
             * Type of data the property contains (e.g. string, int, bytes, etc).
             *
             * <p>This property must be set.
             */
            @NonNull
            public PropertyConfig.Builder setDataType(@DataType int dataType) {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkArgumentInRange(
                        dataType, DATA_TYPE_STRING, DATA_TYPE_DOCUMENT, "dataType");
                mBundle.putInt(DATA_TYPE_FIELD, dataType);
                return this;
            }

            /**
             * The logical schema-type of the contents of this property.
             *
             * <p>Only required when {@link #setDataType} is set to
             * {@link #DATA_TYPE_DOCUMENT}. Otherwise, it is ignored.
             */
            @NonNull
            public PropertyConfig.Builder setSchemaType(@NonNull String schemaType) {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkNotNull(schemaType);
                mBundle.putString(SCHEMA_TYPE_FIELD, schemaType);
                return this;
            }

            /**
             * The cardinality of the property (whether it is optional, required or repeated).
             *
             * <p>This property must be set.
             */
            @NonNull
            public PropertyConfig.Builder setCardinality(@Cardinality int cardinality) {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkArgumentInRange(
                        cardinality, CARDINALITY_REPEATED, CARDINALITY_REQUIRED, "cardinality");
                mBundle.putInt(CARDINALITY_FIELD, cardinality);
                return this;
            }

            /**
             * Configures how a property should be indexed so that it can be retrieved by queries.
             */
            @NonNull
            public PropertyConfig.Builder setIndexingType(@IndexingType int indexingType) {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkArgumentInRange(
                        indexingType, INDEXING_TYPE_NONE, INDEXING_TYPE_PREFIXES, "indexingType");
                mBundle.putInt(INDEXING_TYPE_FIELD, indexingType);
                return this;
            }

            /** Configures how this property should be tokenized (split into words). */
            @NonNull
            public PropertyConfig.Builder setTokenizerType(@TokenizerType int tokenizerType) {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkArgumentInRange(
                        tokenizerType, TOKENIZER_TYPE_NONE, TOKENIZER_TYPE_PLAIN, "tokenizerType");
                mBundle.putInt(TOKENIZER_TYPE_FIELD, tokenizerType);
                return this;
            }

            /**
             * Constructs a new {@link PropertyConfig} from the contents of this builder.
             *
             * <p>After calling this method, the builder must no longer be used.
             *
             * @throws IllegalSchemaException If the property is not correctly populated (e.g.
             *     missing {@code dataType}).
             */
            @NonNull
            public PropertyConfig build() {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                // TODO(b/147692920): Send the schema to Icing Lib for official validation, instead
                //     of partially reimplementing some of the validation Icing does here.
                if (!mBundle.containsKey(DATA_TYPE_FIELD)) {
                    throw new IllegalSchemaException("Missing field: dataType");
                }
                if (mBundle.getString(SCHEMA_TYPE_FIELD, "").isEmpty()
                        && mBundle.getInt(DATA_TYPE_FIELD) == DATA_TYPE_DOCUMENT) {
                    throw new IllegalSchemaException(
                            "Missing field: schemaType (required for configs with "
                                    + "dataType = DOCUMENT)");
                }
                if (!mBundle.containsKey(CARDINALITY_FIELD)) {
                    throw new IllegalSchemaException("Missing field: cardinality");
                }
                mBuilt = true;
                return new PropertyConfig(mName, mBundle);
            }
        }
    }
}
