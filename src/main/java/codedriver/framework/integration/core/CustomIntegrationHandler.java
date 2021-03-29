package codedriver.framework.integration.core;

import java.util.List;

import org.springframework.stereotype.Component;

import codedriver.framework.integration.dto.IntegrationVo;
import codedriver.framework.integration.dto.PatternVo;

@Component
public class CustomIntegrationHandler extends IntegrationHandlerBase {
	public String getName() {
		return Type.CUSTOM.getText();
	}

	@Override
	public String getType() {
		return Type.CUSTOM.getValue();
	}

	public Object myGetData() {
		return null;
	}

	@Override
	public List<PatternVo> getInputPattern() {
		return null;
	}

	@Override
	public List<PatternVo> getOutputPattern() {
		return null;
	}

	@Override
	protected void beforeSend(IntegrationVo integrationVo) {

	}

	@Override
	protected void afterReturn(IntegrationVo integrationVo) {

	}

	@Override
	public Integer hasPattern() {
		return 0;
	}

}
