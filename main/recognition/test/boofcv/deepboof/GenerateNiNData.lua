-- Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
--
-- This file is part of BoofCV (http://boofcv.org).
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
--
-- This file is part of BoofCV (http://boofcv.org).
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
--
-- This file is part of BoofCV (http://boofcv.org).
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

require 'torch'
require 'nn'

local file_path = '../../../../../nin_bn_final.t7'
local output_dir = "."

torch.setdefaulttensortype("torch.FloatTensor")

local net = torch.load(file_path):unpack():float()

local input = torch.randn(1,3,224,224)
torch.save(paths.concat(output_dir,'nin_input'), input)

local output = net:forward(input)

torch.save(paths.concat(output_dir,'nin_output'), output)
