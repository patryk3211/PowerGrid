/*
 * Copyright 2025 'Rory W. J. Smithee'
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.config;

import net.createmod.catnip.config.ConfigBase;

public class CPD extends ConfigBase {
	/* Will you show up or will you just die? */
	public final ConfigFloat rotorKp = f(0, 0.75f, "rotorKp", Comments.rotorKp);
	
	public final ConfigFloat rotorKd = f(0f, 10f, "rotorKd", Comments.rotorKd);

    @Override
    public String getName() {
        return "pdcontroller";
    }

	private static class Comments {
				public static final String rotorKp = "Factor to scale the Proportional factor of the Rotor's force calculation";
		public static final String rotorKd = "Factor to scale the Differential factor of the Rotor's force calculation";
	}
}
