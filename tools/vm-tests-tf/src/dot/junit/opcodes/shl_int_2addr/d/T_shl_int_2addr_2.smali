.source "T_shl_int_2addr_2.java"
.class  public Ldot/junit/opcodes/shl_int_2addr/d/T_shl_int_2addr_2;
.super  Ljava/lang/Object;


.method public constructor <init>()V
.registers 1

       invoke-direct {v0}, Ljava/lang/Object;-><init>()V
       return-void
.end method

.method public run(II)I
.registers 8

       shl-int/2addr v7, v8
       return v7
.end method
