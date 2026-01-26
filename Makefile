PWD:=$(shell pwd)
MILL=$(PWD)/mill
BUILD_DIR=$(PWD)/build

SIM_RTL_DIR=$(BUILD_DIR)/rtl
GEN_SRC_DIR=$(BUILD_DIR)/generated-src

sim-rtl: $(MILL)
	@mkdir -p $(SIM_RTL_DIR)
	$(MILL) xcore.run --target-dir $(SIM_RTL_DIR) --generated-dir $(GEN_SRC_DIR)

check-format:
	$(MILL) checkFormat

reformat:
	$(MILL) reformat

clean:
	rm -rf $(BUILD_DIR)


.PHONY: sim-rtl clean