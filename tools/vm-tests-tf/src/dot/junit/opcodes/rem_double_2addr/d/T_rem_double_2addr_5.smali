.source "T_rem_double_2addr_5.java"
.class  public Ldot/junit/opcodes/rem_double_2addr/d/T_rem_double_2addr_5;
.super  Ljava/lang/Object;


.method public constructor <init>()V
.registers 1

       invoke-direct {v0}, Ljava/lang/Object;-><init>()V
       return-void
.end method

.method public run(DD)D
.registers 14

       rem-double/2addr v9, v12
       return-wide v9
.end method
