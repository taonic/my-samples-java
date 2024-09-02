// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: addressbook.proto

// Protobuf Java Version: 3.25.3
package io.temporal.samples.proto;

/**
 * Protobuf type {@code tutorial.Namespace}
 */
public final class Namespace extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:tutorial.Namespace)
    NamespaceOrBuilder {
private static final long serialVersionUID = 0L;
  // Use Namespace.newBuilder() to construct.
  private Namespace(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Namespace() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new Namespace();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return io.temporal.samples.proto.AddressBookProtos.internal_static_tutorial_Namespace_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return io.temporal.samples.proto.AddressBookProtos.internal_static_tutorial_Namespace_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.temporal.samples.proto.Namespace.class, io.temporal.samples.proto.Namespace.Builder.class);
  }

  private int valueCase_ = 0;
  @SuppressWarnings("serial")
  private java.lang.Object value_;
  public enum ValueCase
      implements com.google.protobuf.Internal.EnumLite,
          com.google.protobuf.AbstractMessage.InternalOneOfEnum {
    JAMID(1),
    GCPSERVICEACCOUNT(2),
    VALUE_NOT_SET(0);
    private final int value;
    private ValueCase(int value) {
      this.value = value;
    }
    /**
     * @param value The number of the enum to look for.
     * @return The enum associated with the given number.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static ValueCase valueOf(int value) {
      return forNumber(value);
    }

    public static ValueCase forNumber(int value) {
      switch (value) {
        case 1: return JAMID;
        case 2: return GCPSERVICEACCOUNT;
        case 0: return VALUE_NOT_SET;
        default: return null;
      }
    }
    public int getNumber() {
      return this.value;
    }
  };

  public ValueCase
  getValueCase() {
    return ValueCase.forNumber(
        valueCase_);
  }

  public static final int JAMID_FIELD_NUMBER = 1;
  /**
   * <code>string jamId = 1;</code>
   * @return Whether the jamId field is set.
   */
  public boolean hasJamId() {
    return valueCase_ == 1;
  }
  /**
   * <code>string jamId = 1;</code>
   * @return The jamId.
   */
  public java.lang.String getJamId() {
    java.lang.Object ref = "";
    if (valueCase_ == 1) {
      ref = value_;
    }
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      if (bs.isValidUtf8() && (valueCase_ == 1)) {
        value_ = s;
      }
      return s;
    }
  }
  /**
   * <code>string jamId = 1;</code>
   * @return The bytes for jamId.
   */
  public com.google.protobuf.ByteString
      getJamIdBytes() {
    java.lang.Object ref = "";
    if (valueCase_ == 1) {
      ref = value_;
    }
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      if (valueCase_ == 1) {
        value_ = b;
      }
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int GCPSERVICEACCOUNT_FIELD_NUMBER = 2;
  /**
   * <code>string gcpServiceAccount = 2;</code>
   * @return Whether the gcpServiceAccount field is set.
   */
  public boolean hasGcpServiceAccount() {
    return valueCase_ == 2;
  }
  /**
   * <code>string gcpServiceAccount = 2;</code>
   * @return The gcpServiceAccount.
   */
  public java.lang.String getGcpServiceAccount() {
    java.lang.Object ref = "";
    if (valueCase_ == 2) {
      ref = value_;
    }
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      if (bs.isValidUtf8() && (valueCase_ == 2)) {
        value_ = s;
      }
      return s;
    }
  }
  /**
   * <code>string gcpServiceAccount = 2;</code>
   * @return The bytes for gcpServiceAccount.
   */
  public com.google.protobuf.ByteString
      getGcpServiceAccountBytes() {
    java.lang.Object ref = "";
    if (valueCase_ == 2) {
      ref = value_;
    }
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      if (valueCase_ == 2) {
        value_ = b;
      }
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (valueCase_ == 1) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, value_);
    }
    if (valueCase_ == 2) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, value_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (valueCase_ == 1) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, value_);
    }
    if (valueCase_ == 2) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, value_);
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof io.temporal.samples.proto.Namespace)) {
      return super.equals(obj);
    }
    io.temporal.samples.proto.Namespace other = (io.temporal.samples.proto.Namespace) obj;

    if (!getValueCase().equals(other.getValueCase())) return false;
    switch (valueCase_) {
      case 1:
        if (!getJamId()
            .equals(other.getJamId())) return false;
        break;
      case 2:
        if (!getGcpServiceAccount()
            .equals(other.getGcpServiceAccount())) return false;
        break;
      case 0:
      default:
    }
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    switch (valueCase_) {
      case 1:
        hash = (37 * hash) + JAMID_FIELD_NUMBER;
        hash = (53 * hash) + getJamId().hashCode();
        break;
      case 2:
        hash = (37 * hash) + GCPSERVICEACCOUNT_FIELD_NUMBER;
        hash = (53 * hash) + getGcpServiceAccount().hashCode();
        break;
      case 0:
      default:
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.temporal.samples.proto.Namespace parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static io.temporal.samples.proto.Namespace parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static io.temporal.samples.proto.Namespace parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.temporal.samples.proto.Namespace parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(io.temporal.samples.proto.Namespace prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code tutorial.Namespace}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:tutorial.Namespace)
      io.temporal.samples.proto.NamespaceOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.temporal.samples.proto.AddressBookProtos.internal_static_tutorial_Namespace_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.temporal.samples.proto.AddressBookProtos.internal_static_tutorial_Namespace_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.temporal.samples.proto.Namespace.class, io.temporal.samples.proto.Namespace.Builder.class);
    }

    // Construct using io.temporal.samples.proto.Namespace.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      valueCase_ = 0;
      value_ = null;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return io.temporal.samples.proto.AddressBookProtos.internal_static_tutorial_Namespace_descriptor;
    }

    @java.lang.Override
    public io.temporal.samples.proto.Namespace getDefaultInstanceForType() {
      return io.temporal.samples.proto.Namespace.getDefaultInstance();
    }

    @java.lang.Override
    public io.temporal.samples.proto.Namespace build() {
      io.temporal.samples.proto.Namespace result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public io.temporal.samples.proto.Namespace buildPartial() {
      io.temporal.samples.proto.Namespace result = new io.temporal.samples.proto.Namespace(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      buildPartialOneofs(result);
      onBuilt();
      return result;
    }

    private void buildPartial0(io.temporal.samples.proto.Namespace result) {
      int from_bitField0_ = bitField0_;
    }

    private void buildPartialOneofs(io.temporal.samples.proto.Namespace result) {
      result.valueCase_ = valueCase_;
      result.value_ = this.value_;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.temporal.samples.proto.Namespace) {
        return mergeFrom((io.temporal.samples.proto.Namespace)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.temporal.samples.proto.Namespace other) {
      if (other == io.temporal.samples.proto.Namespace.getDefaultInstance()) return this;
      switch (other.getValueCase()) {
        case JAMID: {
          valueCase_ = 1;
          value_ = other.value_;
          onChanged();
          break;
        }
        case GCPSERVICEACCOUNT: {
          valueCase_ = 2;
          value_ = other.value_;
          onChanged();
          break;
        }
        case VALUE_NOT_SET: {
          break;
        }
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              valueCase_ = 1;
              value_ = bs;
              break;
            } // case 10
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              valueCase_ = 2;
              value_ = bs;
              break;
            } // case 18
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int valueCase_ = 0;
    private java.lang.Object value_;
    public ValueCase
        getValueCase() {
      return ValueCase.forNumber(
          valueCase_);
    }

    public Builder clearValue() {
      valueCase_ = 0;
      value_ = null;
      onChanged();
      return this;
    }

    private int bitField0_;

    /**
     * <code>string jamId = 1;</code>
     * @return Whether the jamId field is set.
     */
    @java.lang.Override
    public boolean hasJamId() {
      return valueCase_ == 1;
    }
    /**
     * <code>string jamId = 1;</code>
     * @return The jamId.
     */
    @java.lang.Override
    public java.lang.String getJamId() {
      java.lang.Object ref = "";
      if (valueCase_ == 1) {
        ref = value_;
      }
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (valueCase_ == 1) {
          if (bs.isValidUtf8()) {
            value_ = s;
          }
        }
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string jamId = 1;</code>
     * @return The bytes for jamId.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getJamIdBytes() {
      java.lang.Object ref = "";
      if (valueCase_ == 1) {
        ref = value_;
      }
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        if (valueCase_ == 1) {
          value_ = b;
        }
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string jamId = 1;</code>
     * @param value The jamId to set.
     * @return This builder for chaining.
     */
    public Builder setJamId(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      valueCase_ = 1;
      value_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string jamId = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearJamId() {
      if (valueCase_ == 1) {
        valueCase_ = 0;
        value_ = null;
        onChanged();
      }
      return this;
    }
    /**
     * <code>string jamId = 1;</code>
     * @param value The bytes for jamId to set.
     * @return This builder for chaining.
     */
    public Builder setJamIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      valueCase_ = 1;
      value_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string gcpServiceAccount = 2;</code>
     * @return Whether the gcpServiceAccount field is set.
     */
    @java.lang.Override
    public boolean hasGcpServiceAccount() {
      return valueCase_ == 2;
    }
    /**
     * <code>string gcpServiceAccount = 2;</code>
     * @return The gcpServiceAccount.
     */
    @java.lang.Override
    public java.lang.String getGcpServiceAccount() {
      java.lang.Object ref = "";
      if (valueCase_ == 2) {
        ref = value_;
      }
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (valueCase_ == 2) {
          if (bs.isValidUtf8()) {
            value_ = s;
          }
        }
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string gcpServiceAccount = 2;</code>
     * @return The bytes for gcpServiceAccount.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getGcpServiceAccountBytes() {
      java.lang.Object ref = "";
      if (valueCase_ == 2) {
        ref = value_;
      }
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        if (valueCase_ == 2) {
          value_ = b;
        }
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string gcpServiceAccount = 2;</code>
     * @param value The gcpServiceAccount to set.
     * @return This builder for chaining.
     */
    public Builder setGcpServiceAccount(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      valueCase_ = 2;
      value_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string gcpServiceAccount = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearGcpServiceAccount() {
      if (valueCase_ == 2) {
        valueCase_ = 0;
        value_ = null;
        onChanged();
      }
      return this;
    }
    /**
     * <code>string gcpServiceAccount = 2;</code>
     * @param value The bytes for gcpServiceAccount to set.
     * @return This builder for chaining.
     */
    public Builder setGcpServiceAccountBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      valueCase_ = 2;
      value_ = value;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:tutorial.Namespace)
  }

  // @@protoc_insertion_point(class_scope:tutorial.Namespace)
  private static final io.temporal.samples.proto.Namespace DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.temporal.samples.proto.Namespace();
  }

  public static io.temporal.samples.proto.Namespace getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  @java.lang.Deprecated public static final com.google.protobuf.Parser<Namespace>
      PARSER = new com.google.protobuf.AbstractParser<Namespace>() {
    @java.lang.Override
    public Namespace parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<Namespace> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<Namespace> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public io.temporal.samples.proto.Namespace getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

