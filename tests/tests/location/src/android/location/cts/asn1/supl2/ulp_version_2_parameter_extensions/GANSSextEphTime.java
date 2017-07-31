/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.location.cts.asn1.supl2.ulp_version_2_parameter_extensions;

/*
 */


//
//
import android.location.cts.asn1.base.Asn1Integer;
import android.location.cts.asn1.base.Asn1Null;
import android.location.cts.asn1.base.Asn1Object;
import android.location.cts.asn1.base.Asn1Sequence;
import android.location.cts.asn1.base.Asn1Tag;
import android.location.cts.asn1.base.BitStream;
import android.location.cts.asn1.base.BitStreamReader;
import android.location.cts.asn1.base.SequenceComponent;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import javax.annotation.Nullable;


/**
*/
public  class GANSSextEphTime extends Asn1Sequence {
  //

  private static final Asn1Tag TAG_GANSSextEphTime
      = Asn1Tag.fromClassAndNumber(-1, -1);

  public GANSSextEphTime() {
    super();
  }

  @Override
  @Nullable
  protected Asn1Tag getTag() {
    return TAG_GANSSextEphTime;
  }

  @Override
  protected boolean isTagImplicit() {
    return true;
  }

  public static Collection<Asn1Tag> getPossibleFirstTags() {
    if (TAG_GANSSextEphTime != null) {
      return ImmutableList.of(TAG_GANSSextEphTime);
    } else {
      return Asn1Sequence.getPossibleFirstTags();
    }
  }

  /**
   * Creates a new GANSSextEphTime from encoded stream.
   */
  public static GANSSextEphTime fromPerUnaligned(byte[] encodedBytes) {
    GANSSextEphTime result = new GANSSextEphTime();
    result.decodePerUnaligned(new BitStreamReader(encodedBytes));
    return result;
  }

  /**
   * Creates a new GANSSextEphTime from encoded stream.
   */
  public static GANSSextEphTime fromPerAligned(byte[] encodedBytes) {
    GANSSextEphTime result = new GANSSextEphTime();
    result.decodePerAligned(new BitStreamReader(encodedBytes));
    return result;
  }



  @Override protected boolean isExtensible() {
    return true;
  }

  @Override public boolean containsExtensionValues() {
    for (SequenceComponent extensionComponent : getExtensionComponents()) {
      if (extensionComponent.isExplicitlySet()) return true;
    }
    return false;
  }

  
  private GANSSextEphTime.gANSSdayType gANSSday_;
  public GANSSextEphTime.gANSSdayType getGANSSday() {
    return gANSSday_;
  }
  /**
   * @throws ClassCastException if value is not a GANSSextEphTime.gANSSdayType
   */
  public void setGANSSday(Asn1Object value) {
    this.gANSSday_ = (GANSSextEphTime.gANSSdayType) value;
  }
  public GANSSextEphTime.gANSSdayType setGANSSdayToNewInstance() {
    gANSSday_ = new GANSSextEphTime.gANSSdayType();
    return gANSSday_;
  }
  
  private GANSSextEphTime.gANSSTODhourType gANSSTODhour_;
  public GANSSextEphTime.gANSSTODhourType getGANSSTODhour() {
    return gANSSTODhour_;
  }
  /**
   * @throws ClassCastException if value is not a GANSSextEphTime.gANSSTODhourType
   */
  public void setGANSSTODhour(Asn1Object value) {
    this.gANSSTODhour_ = (GANSSextEphTime.gANSSTODhourType) value;
  }
  public GANSSextEphTime.gANSSTODhourType setGANSSTODhourToNewInstance() {
    gANSSTODhour_ = new GANSSextEphTime.gANSSTODhourType();
    return gANSSTODhour_;
  }
  

  

  

  @Override public Iterable<? extends SequenceComponent> getComponents() {
    ImmutableList.Builder<SequenceComponent> builder = ImmutableList.builder();
    
    builder.add(new SequenceComponent() {
          Asn1Tag tag = Asn1Tag.fromClassAndNumber(2, 0);

          @Override public boolean isExplicitlySet() {
            return getGANSSday() != null;
          }

          @Override public boolean hasDefaultValue() {
            return false;
          }

          @Override public boolean isOptional() {
            return false;
          }

          @Override public Asn1Object getComponentValue() {
            return getGANSSday();
          }

          @Override public void setToNewInstance() {
            setGANSSdayToNewInstance();
          }

          @Override public Collection<Asn1Tag> getPossibleFirstTags() {
            return tag == null ? GANSSextEphTime.gANSSdayType.getPossibleFirstTags() : ImmutableList.of(tag);
          }

          @Override
          public Asn1Tag getTag() {
            return tag;
          }

          @Override
          public boolean isImplicitTagging() {
            return true;
          }

          @Override public String toIndentedString(String indent) {
                return "gANSSday : "
                    + getGANSSday().toIndentedString(indent);
              }
        });
    
    builder.add(new SequenceComponent() {
          Asn1Tag tag = Asn1Tag.fromClassAndNumber(2, 1);

          @Override public boolean isExplicitlySet() {
            return getGANSSTODhour() != null;
          }

          @Override public boolean hasDefaultValue() {
            return false;
          }

          @Override public boolean isOptional() {
            return false;
          }

          @Override public Asn1Object getComponentValue() {
            return getGANSSTODhour();
          }

          @Override public void setToNewInstance() {
            setGANSSTODhourToNewInstance();
          }

          @Override public Collection<Asn1Tag> getPossibleFirstTags() {
            return tag == null ? GANSSextEphTime.gANSSTODhourType.getPossibleFirstTags() : ImmutableList.of(tag);
          }

          @Override
          public Asn1Tag getTag() {
            return tag;
          }

          @Override
          public boolean isImplicitTagging() {
            return true;
          }

          @Override public String toIndentedString(String indent) {
                return "gANSSTODhour : "
                    + getGANSSTODhour().toIndentedString(indent);
              }
        });
    
    return builder.build();
  }

  @Override public Iterable<? extends SequenceComponent>
                                                    getExtensionComponents() {
    ImmutableList.Builder<SequenceComponent> builder = ImmutableList.builder();
      
      return builder.build();
    }

  
/*
 */


//

/**
 */
public static class gANSSdayType extends Asn1Integer {
  //

  private static final Asn1Tag TAG_gANSSdayType
      = Asn1Tag.fromClassAndNumber(-1, -1);

  public gANSSdayType() {
    super();
    setValueRange("0", "8191");

  }

  @Override
  @Nullable
  protected Asn1Tag getTag() {
    return TAG_gANSSdayType;
  }

  @Override
  protected boolean isTagImplicit() {
    return true;
  }

  public static Collection<Asn1Tag> getPossibleFirstTags() {
    if (TAG_gANSSdayType != null) {
      return ImmutableList.of(TAG_gANSSdayType);
    } else {
      return Asn1Integer.getPossibleFirstTags();
    }
  }

  /**
   * Creates a new gANSSdayType from encoded stream.
   */
  public static gANSSdayType fromPerUnaligned(byte[] encodedBytes) {
    gANSSdayType result = new gANSSdayType();
    result.decodePerUnaligned(new BitStreamReader(encodedBytes));
    return result;
  }

  /**
   * Creates a new gANSSdayType from encoded stream.
   */
  public static gANSSdayType fromPerAligned(byte[] encodedBytes) {
    gANSSdayType result = new gANSSdayType();
    result.decodePerAligned(new BitStreamReader(encodedBytes));
    return result;
  }

  @Override public Iterable<BitStream> encodePerUnaligned() {
    return super.encodePerUnaligned();
  }

  @Override public Iterable<BitStream> encodePerAligned() {
    return super.encodePerAligned();
  }

  @Override public void decodePerUnaligned(BitStreamReader reader) {
    super.decodePerUnaligned(reader);
  }

  @Override public void decodePerAligned(BitStreamReader reader) {
    super.decodePerAligned(reader);
  }

  @Override public String toString() {
    return toIndentedString("");
  }

  public String toIndentedString(String indent) {
    return "gANSSdayType = " + getInteger() + ";\n";
  }
}

  
/*
 */


//

/**
 */
public static class gANSSTODhourType extends Asn1Integer {
  //

  private static final Asn1Tag TAG_gANSSTODhourType
      = Asn1Tag.fromClassAndNumber(-1, -1);

  public gANSSTODhourType() {
    super();
    setValueRange("0", "23");

  }

  @Override
  @Nullable
  protected Asn1Tag getTag() {
    return TAG_gANSSTODhourType;
  }

  @Override
  protected boolean isTagImplicit() {
    return true;
  }

  public static Collection<Asn1Tag> getPossibleFirstTags() {
    if (TAG_gANSSTODhourType != null) {
      return ImmutableList.of(TAG_gANSSTODhourType);
    } else {
      return Asn1Integer.getPossibleFirstTags();
    }
  }

  /**
   * Creates a new gANSSTODhourType from encoded stream.
   */
  public static gANSSTODhourType fromPerUnaligned(byte[] encodedBytes) {
    gANSSTODhourType result = new gANSSTODhourType();
    result.decodePerUnaligned(new BitStreamReader(encodedBytes));
    return result;
  }

  /**
   * Creates a new gANSSTODhourType from encoded stream.
   */
  public static gANSSTODhourType fromPerAligned(byte[] encodedBytes) {
    gANSSTODhourType result = new gANSSTODhourType();
    result.decodePerAligned(new BitStreamReader(encodedBytes));
    return result;
  }

  @Override public Iterable<BitStream> encodePerUnaligned() {
    return super.encodePerUnaligned();
  }

  @Override public Iterable<BitStream> encodePerAligned() {
    return super.encodePerAligned();
  }

  @Override public void decodePerUnaligned(BitStreamReader reader) {
    super.decodePerUnaligned(reader);
  }

  @Override public void decodePerAligned(BitStreamReader reader) {
    super.decodePerAligned(reader);
  }

  @Override public String toString() {
    return toIndentedString("");
  }

  public String toIndentedString(String indent) {
    return "gANSSTODhourType = " + getInteger() + ";\n";
  }
}

  

    

  @Override public Iterable<BitStream> encodePerUnaligned() {
    return super.encodePerUnaligned();
  }

  @Override public Iterable<BitStream> encodePerAligned() {
    return super.encodePerAligned();
  }

  @Override public void decodePerUnaligned(BitStreamReader reader) {
    super.decodePerUnaligned(reader);
  }

  @Override public void decodePerAligned(BitStreamReader reader) {
    super.decodePerAligned(reader);
  }

  @Override public String toString() {
    return toIndentedString("");
  }

  public String toIndentedString(String indent) {
    StringBuilder builder = new StringBuilder();
    builder.append("GANSSextEphTime = {\n");
    final String internalIndent = indent + "  ";
    for (SequenceComponent component : getComponents()) {
      if (component.isExplicitlySet()) {
        builder.append(internalIndent)
            .append(component.toIndentedString(internalIndent));
      }
    }
    if (isExtensible()) {
      builder.append(internalIndent).append("...\n");
      for (SequenceComponent component : getExtensionComponents()) {
        if (component.isExplicitlySet()) {
          builder.append(internalIndent)
              .append(component.toIndentedString(internalIndent));
        }
      }
    }
    builder.append(indent).append("};\n");
    return builder.toString();
  }
}
