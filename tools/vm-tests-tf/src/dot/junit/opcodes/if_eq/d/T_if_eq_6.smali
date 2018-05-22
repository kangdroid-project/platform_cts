# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

.source "T_if_eq_6.java"
.class  public Ldot/junit/opcodes/if_eq/d/T_if_eq_6;
.super  Ljava/lang/Object;


.method public constructor <init>()V
.registers 1

       invoke-direct {v0}, Ljava/lang/Object;-><init>()V
       return-void
.end method

.method public run(Ljava/lang/String;Ljava/lang/String;)I
.registers 8

       if-eq v6, v8, :Label11
       const/16 v6, 1234
:Label10
       return v6
       
:Label11
       const/4 v6, 1
       goto :Label10
.end method
