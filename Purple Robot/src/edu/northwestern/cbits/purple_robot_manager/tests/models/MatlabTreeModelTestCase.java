package edu.northwestern.cbits.purple_robot_manager.tests.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import android.content.Context;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.models.trees.LeafNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.TreeNodeParser;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.TreeNodeParser.ParserNotFound;
import edu.northwestern.cbits.purple_robot_manager.tests.RobotTestCase;

public class MatlabTreeModelTestCase extends RobotTestCase 
{
	public MatlabTreeModelTestCase(Context context, int priority) 
	{
		super(context, priority);
	}
	
	public void test() 
	{
		if (this.isSelected(this._context) == false)
			return;

		try 
		{
		    StringBuilder sb = new StringBuilder();
		    
		    InputStream file = this._context.getAssets().open("test_data/matlab-tree.txt");
		    BufferedReader in = new BufferedReader(new InputStreamReader(file, "UTF-8"));

		    String line = null;

		    while ((line = in.readLine()) != null) 
		    {
		      sb.append(line + "\n");
		    }

		    in.close();
			
			TreeNode node = TreeNodeParser.parseString(sb.toString());
			System.out.println(node.toString(0));
			
			HashMap<String, Object> world = new HashMap<String, Object>();
			
			Map<String, Object> prediction = node.fetchPrediction(world);

			world.put("x59", Double.valueOf(0.1));
			prediction = node.fetchPrediction(world);
//			System.out.println("Expect 3. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 
			Assert.assertEquals("x59", "3", prediction.get(LeafNode.PREDICTION));

			world.put("x59", "0.02");
			prediction = node.fetchPrediction(world);
//			System.out.println("Expect 2. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 
			Assert.assertEquals("x59", "2", prediction.get(LeafNode.PREDICTION));

			world.put("x39", "0.16");
			prediction = node.fetchPrediction(world);
//			System.out.println("Expect 4. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 
			Assert.assertEquals("x39", "4", prediction.get(LeafNode.PREDICTION));			
			
			world.put("x39", Double.valueOf(20));
			prediction = node.fetchPrediction(world);
//			System.out.println("Expect 5. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 
			Assert.assertEquals("x39", "5", prediction.get(LeafNode.PREDICTION));
		} 
		catch (ParserNotFound e) 
		{
			e.printStackTrace();
			Assert.fail("x59");
		} 
		catch (TreeNodeException e) 
		{
			e.printStackTrace();
			Assert.fail("x39");
		} 
	}
	
	public int estimatedMinutes() 
	{
		return 1;
	}

	public String name(Context context) 
	{
		return context.getString(R.string.name_matlab_tree_model_test);
	}
}